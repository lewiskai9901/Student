/**
 * V7 检查平台 - 语音转文字 composable
 * 使用 Web Speech API (SpeechRecognition) 实现浏览器端语音输入
 */
import { ref, onUnmounted } from 'vue'

interface SpeechRecognitionOptions {
  lang?: string
  continuous?: boolean
  interimResults?: boolean
}

export function useSpeechRecognition(options: SpeechRecognitionOptions = {}) {
  const {
    lang = 'zh-CN',
    continuous = true,
    interimResults = true,
  } = options

  const isListening = ref(false)
  const transcript = ref('')
  const interimTranscript = ref('')
  const error = ref<string | null>(null)
  const isSupported = ref(false)

  let recognition: any = null

  // Check support
  const SpeechRecognitionAPI =
    (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  isSupported.value = !!SpeechRecognitionAPI

  if (SpeechRecognitionAPI) {
    recognition = new SpeechRecognitionAPI()
    recognition.lang = lang
    recognition.continuous = continuous
    recognition.interimResults = interimResults

    recognition.onresult = (event: any) => {
      let finalText = ''
      let interimText = ''

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        if (result.isFinal) {
          finalText += result[0].transcript
        } else {
          interimText += result[0].transcript
        }
      }

      if (finalText) {
        transcript.value += finalText
      }
      interimTranscript.value = interimText
    }

    recognition.onerror = (event: any) => {
      error.value = event.error
      if (event.error !== 'no-speech') {
        isListening.value = false
      }
    }

    recognition.onend = () => {
      // Auto-restart if still supposed to be listening
      if (isListening.value && continuous) {
        try {
          recognition.start()
        } catch {
          isListening.value = false
        }
      } else {
        isListening.value = false
      }
    }
  }

  function startListening() {
    if (!isSupported.value || !recognition) {
      error.value = '当前浏览器不支持语音识别'
      return
    }
    error.value = null
    interimTranscript.value = ''
    try {
      recognition.start()
      isListening.value = true
    } catch {
      error.value = '启动语音识别失败'
    }
  }

  function stopListening() {
    if (recognition && isListening.value) {
      isListening.value = false
      recognition.stop()
    }
  }

  function resetTranscript() {
    transcript.value = ''
    interimTranscript.value = ''
  }

  onUnmounted(() => {
    stopListening()
  })

  return {
    isSupported,
    isListening,
    transcript,
    interimTranscript,
    error,
    startListening,
    stopListening,
    resetTranscript,
  }
}
