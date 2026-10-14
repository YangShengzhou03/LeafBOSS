import { ref, onMounted, onUnmounted } from 'vue'

export function useIsMobile(breakpoint = 768) {
  const isMobile = ref(window.innerWidth <= breakpoint)
  const update = () => {
    isMobile.value = window.innerWidth <= breakpoint
  }
  onMounted(() => window.addEventListener('resize', update))
  onUnmounted(() => window.removeEventListener('resize', update))
  return isMobile
}
