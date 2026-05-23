/*****YTPRO*******
Author: Prateek Chaubey
Version: 3.9.8
URI: https://github.com/prateek-chaubey/YTPRO
Last Updated On: 1 May , 2026 , 19:25 IST
*/


window.ytproSabrDownload= async function() {


var ytproDownDiv=getDownloadElement();

ytproDownDiv.querySelector("#videoViewDiv").innerHTML="Loading...";


//Get Video ID
var videoId ="";

if(window.location.pathname.indexOf("shorts") > -1){
videoId=window.location.pathname.substr(8,window.location.pathname.length);
}
else{
videoId=new URLSearchParams(window.location.search).get("v");
}


//videoId="vY31qIX7LzQ";


if (!videoId) { window.Android?.showToast?.('No video ID found in URL.'); return; }

// Imports
const { Innertube, Platform, Constants } = await import(
'https://cdn.jsdelivr.net/npm/youtubei.js@17.0.1/bundle/browser.min.js'
);
const { SabrStream } = await import('https://esm.sh/googlevideo@4.0.4/sabr-stream');
const { buildSabrFormat , EnabledTrackTypes } = await import('https://esm.sh/googlevideo@4.0.4/utils');
const { BG, buildURL, getHeaders } = await import('https://esm.sh/bgutils-js@3.2.0');

Platform.shim.eval = async (data, env) => {
const props = [];
if (env.n)   props.push(`n: exportedVars.nFunction("${env.n}")`);
if (env.sig) props.push(`sig: exportedVars.sigFunction("${env.sig}")`);
return new Function(`${data.output}\nreturn { ${props.join(', ')} }`)();
};

// Create Innertube (WEB Client Setup & Proxy)
const cookies = window.Android?.getAllCookies?.('https://www.youtube.com') ?? '';

const yt = await Innertube.create({
cookie: cookies,
retrieve_player: true,
generate_session_locally: true,
fetch: async (input, init = {}) => {

const reqUrl = input instanceof Request ? input.url : input.toString();
const url    = new URL(reqUrl);
const method = init.method ?? (input instanceof Request ? input.method : 'GET');
const headers = new Headers();

if (input instanceof Request) input.headers.forEach((v, k) => headers.set(k, v));
if (init.headers) new Headers(init.headers).forEach((v, k) => headers.set(k, v));

headers.set('User-Agent', "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
headers.set('Sec-Ch-Ua', '"Chromium";v="124", "Google Chrome";v="124", "Not-A.Brand";v="99"');
headers.set('Sec-Ch-Ua-Mobile', '?0');
headers.set('Sec-Ch-Ua-Platform', '"Windows"');

const proxyUrl = `https://youtube.com/ytpro_cdn/proxy?url=${encodeURIComponent(reqUrl)}&method=${method}`;

const body = init.body || (input instanceof Request ? await input.clone().text() : null);

try {
const response = await fetch(proxyUrl, {
...init,
method: 'POST',
headers: { 'Content-Type': 'application/json' },
body: JSON.stringify({
headers: Object.fromEntries(headers.entries()),
body: body
})
});

const data = await response.json();
return new Response(data.stream, {
status: 200,
headers: { 'Content-Type': 'application/json' }
});
} catch (e) {
return fetch(input, init);
}
}
});

// Get Video Info
const info = await yt.getInfo(videoId);

// Filter Formats (Adaptive Only for High Res)
const formats = info.streaming_data.adaptive_formats.filter(f => f.has_video);
const audioFormats = info.streaming_data.adaptive_formats.filter(f => f.has_audio);

// UI Generation
const videoDiv = ytproDownDiv.querySelector("#videoViewDiv");
videoDiv.innerHTML = `
<div style="padding:10px; color:white; font-family:sans-serif;">
<h3 style="margin-top:0;">Download Video</h3>
<p>${info.basic_info.title}</p>

<div style="margin-bottom:15px;">
<label>Video Quality:</label><br/>
<select id="videoQual" style="width:100%; padding:8px; margin-top:5px; background:#333; color:white; border:1px solid #555; border-radius:4px;">
${formats.map((f, i) => `<option value="${i}">${f.quality_label} (${f.mime_type.split(';')[0]}) - ${Utils.formatBytes(f.content_length)}</option>`).join('')}
</select>
</div>

<div style="margin-bottom:15px;">
<label>Audio Quality:</label><br/>
<select id="audioQual" style="width:100%; padding:8px; margin-top:5px; background:#333; color:white; border:1px solid #555; border-radius:4px;">
${audioFormats.map((f, i) => `<option value="${i}">${f.audio_quality} (${f.mime_type.split(';')[0]}) - ${Utils.formatBytes(f.content_length)}</option>`).join('')}
</select>
</div>

<button id="startDownload" style="width:100%; padding:12px; background:#f00; color:white; border:none; border-radius:4px; font-weight:bold; cursor:pointer;">
START DOWNLOAD
</button>

<div id="dlProgress" style="margin-top:20px; display:none;">
<div style="width:100%; height:10px; background:#444; border-radius:5px; overflow:hidden;">
<div id="progressBar" style="width:0%; height:100%; background:#0f0; transition:width 0.3s;"></div>
</div>
<p id="progressText" style="text-align:center; margin-top:5px; font-size:12px;">Initializing...</p>
</div>
</div>
`;

videoDiv.querySelector("#startDownload").onclick = async () => {
const vIdx = videoDiv.querySelector("#videoQual").value;
const aIdx = videoDiv.querySelector("#audioQual").value;
const videoFormat = formats[vIdx];
const audioFormat = audioFormats[aIdx];

videoDiv.querySelector("#startDownload").disabled = true;
videoDiv.querySelector("#dlProgress").style.display = "block";

try {
// Filenames
const safeTitle = info.basic_info.title.replace(/[^\w\s]/gi, '');
const vExt = videoFormat.mime_type.includes('webm') ? 'webm' : 'mp4';
const aExt = audioFormat.mime_type.includes('webm') ? 'webm' : 'm4a';

const videoFile = `${safeTitle}_video.${vExt}`;
const audioFile = `${safeTitle}_audio.${aExt}`;
const outputFile = `${safeTitle}.${vExt}`;

// Download Video
await downloadAdaptivePart(videoFormat, videoFile, "Video");

// Download Audio
await downloadAdaptivePart(audioFormat, audioFile, "Audio");

// Muxing
videoDiv.querySelector("#progressText").innerText = "Muxing Video and Audio...";
window.Android?.muxVideoAudio?.(videoFile, audioFile, outputFile);

} catch (e) {
console.error(e);
window.Android?.showToast?.("Download failed: " + e.message);
videoDiv.querySelector("#startDownload").disabled = false;
}
};

async function downloadAdaptivePart(format, fileName, type) {
if (!window.Android?.isWebViewSupported?.()) {
throw new Error("SABR download requires Android WebView 120+");
}

window.Android?.requestBinaryPort?.(fileName);

return new Promise(async (resolve, reject) => {

// Wait for the port from Android
const onPortMessage = async (e) => {
if (typeof e.data === 'string' && e.data.startsWith("PORT_FOR:" + fileName)) {
window.removeEventListener('message', onPortMessage);
const port = e.ports[0];

try {
const stream = await yt.download(videoId, {
format: format,
type: 'video+audio'
});

const reader = stream.getReader();
let downloaded = 0;
const total = format.content_length;

while (true) {
const { done, value } = await reader.read();
if (done) break;

port.postMessage(value.buffer, [value.buffer]);
downloaded += value.length;

const pc = Math.round((downloaded / total) * 100);
videoDiv.querySelector("#progressBar").style.width = pc + "%";
videoDiv.querySelector("#progressText").innerText = `Downloading ${type}: ${pc}% (${Utils.formatBytes(downloaded)} / ${Utils.formatBytes(total)})`;
}

port.postMessage("END");
resolve();
} catch (err) {
reject(err);
}
}
};

window.addEventListener('message', onPortMessage);
});
}
};
