import { computed, ref } from 'vue'

type ThemeMode = 'dark' | 'light'

// Module-level singleton so every component sees the same state.
const themeMode = ref<ThemeMode>('dark')
let initialized = false

function applyTheme(mode: ThemeMode) {
  document.body.classList.remove('theme-dark', 'theme-light')
  document.body.classList.add(mode === 'dark' ? 'theme-dark' : 'theme-light')
}

function initTheme() {
  if (initialized) return
  initialized = true
  try {
    const saved = localStorage.getItem('themeMode') as ThemeMode | null
    if (saved === 'dark' || saved === 'light') themeMode.value = saved
  } catch {}
  applyTheme(themeMode.value)
}

export function useTheme() {
  initTheme()

  const isDark = computed(() => themeMode.value === 'dark')

  function setTheme(mode: ThemeMode) {
    themeMode.value = mode
    applyTheme(mode)
    try { localStorage.setItem('themeMode', mode) } catch {}
  }

  function toggleTheme() {
    setTheme(themeMode.value === 'dark' ? 'light' : 'dark')
  }

  return { themeMode, isDark, setTheme, toggleTheme }
}

