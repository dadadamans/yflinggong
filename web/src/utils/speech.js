let voiceCache = [];

const loadVoices = () => {
  if (!window.speechSynthesis) return [];
  const voices = window.speechSynthesis.getVoices();
  voiceCache = voices;
  return voices;
};

const getBestChineseVoice = () => {
  if (!window.speechSynthesis) return null;

  let voices = window.speechSynthesis.getVoices();
  if (voices.length === 0) {
    voices = voiceCache;
  }
  if (voices.length === 0) return null;

  return voices.find(v => v.lang === 'zh-CN')
    || voices.find(v => v.lang === 'zh-TW')
    || voices.find(v => v.lang.includes('zh'))
    || null;
};

const speak = (text) => {
  if (!window.speechSynthesis) {
    console.error("浏览器不支持语音合成");
    return;
  }

  const textStr = String(text ?? "").trim();
  if (!textStr) {
    console.warn("播报内容为空");
    return;
  }

  window.speechSynthesis.cancel();

  const utterance = new SpeechSynthesisUtterance(textStr);

  const voice = getBestChineseVoice();
  if (voice) {
    utterance.voice = voice;
  } else {
    utterance.lang = 'zh-CN';
    console.warn("未找到中文语音，使用默认 lang=zh-CN");
  }

  utterance.rate = 1.0;

  utterance.onerror = (e) => {
    console.error("语音播报错误:", e.error);
  };

  window.speechSynthesis.speak(utterance);
};

const stopSpeak = () => {
  window.speechSynthesis?.cancel();
};

const initSpeech = () => {
  if (!window.speechSynthesis) return;
  
  const dummy = new SpeechSynthesisUtterance('');
  dummy.lang = 'zh-CN';
  dummy.rate = 0.01;
  window.speechSynthesis.speak(dummy);
  window.speechSynthesis.cancel();
  
  loadVoices();
  window.speechSynthesis.onvoiceschanged = () => {
    loadVoices();
  };
};

if (typeof window !== 'undefined' && window.speechSynthesis) {
  initSpeech();
}

export { speak, stopSpeak };
