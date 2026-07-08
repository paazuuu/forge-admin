<template>
  <Teleport to="body">
    <Transition name="global-loading-fade">
      <div
        v-if="active"
        class="global-loading-overlay"
        role="status"
        aria-live="polite"
        aria-busy="true"
      >
        <div class="global-loading-panel">
          <div class="global-loading-spinner" aria-hidden="true" />
          <div class="global-loading-text">
            {{ message }}
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue'
import { useGlobalLoading } from '@/composables/useGlobalLoading'

const { active, message } = useGlobalLoading()

const blockedEvents = [
  'click',
  'dblclick',
  'mousedown',
  'mouseup',
  'pointerdown',
  'pointerup',
  'touchstart',
  'touchmove',
  'wheel',
  'keydown',
]

let listenersAttached = false

function blockInteraction(event) {
  if (!active.value)
    return

  event.preventDefault()
  event.stopPropagation()
  event.stopImmediatePropagation?.()
}

function attachInteractionGuards() {
  if (listenersAttached || typeof document === 'undefined')
    return

  blockedEvents.forEach((eventName) => {
    document.addEventListener(eventName, blockInteraction, { capture: true, passive: false })
  })
  listenersAttached = true
}

function detachInteractionGuards() {
  if (!listenersAttached || typeof document === 'undefined')
    return

  blockedEvents.forEach((eventName) => {
    document.removeEventListener(eventName, blockInteraction, { capture: true })
  })
  listenersAttached = false
}

watch(active, (locked) => {
  if (locked)
    attachInteractionGuards()
  else
    detachInteractionGuards()
}, { immediate: true })

onBeforeUnmount(() => {
  detachInteractionGuards()
})
</script>

<style scoped>
.global-loading-overlay {
  position: fixed;
  inset: 0;
  z-index: 100000;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(
      circle at center,
      color-mix(in srgb, var(--primary-color, #165dff) 9%, transparent),
      transparent 35%
    ),
    color-mix(in srgb, var(--bg-primary, #ffffff) 70%, transparent);
  backdrop-filter: blur(2px);
  pointer-events: auto;
  user-select: none;
}

.global-loading-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-width: 180px;
  padding: 24px 28px;
  color: var(--text-primary, #1d2129);
}

.global-loading-spinner {
  --loader-color: var(--primary-color, #165dff);

  width: 46px;
  height: 46px;
  background: var(--loader-color);
  border-radius: 10%;
  box-shadow: 0 12px 34px color-mix(in srgb, var(--loader-color) 34%, transparent);
  animation: global-loading-rotate 1s linear infinite;
}

.global-loading-text {
  max-width: min(70vw, 420px);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
  color: var(--text-secondary, #4e5969);
  text-align: center;
  letter-spacing: 0;
  word-break: break-word;
}

.global-loading-fade-enter-active,
.global-loading-fade-leave-active {
  transition: opacity 0.16s ease;
}

.global-loading-fade-enter-from,
.global-loading-fade-leave-to {
  opacity: 0;
}

:global(html.forge-global-loading-locked),
:global(body.forge-global-loading-locked) {
  overflow: hidden;
}

:global(.dark) .global-loading-overlay {
  background:
    radial-gradient(
      circle at center,
      color-mix(in srgb, var(--primary-color, #4080ff) 18%, transparent),
      transparent 36%
    ),
    color-mix(in srgb, var(--bg-primary, #0f172a) 78%, transparent);
}

:global(.dark) .global-loading-text {
  color: var(--text-secondary, #c9d1d9);
}

@keyframes global-loading-rotate {
  0% {
    transform: rotate(0deg) scale(0.2);
    border-radius: 10%;
  }

  50% {
    transform: rotate(180deg) scale(1.45);
    border-radius: 50%;
  }

  100% {
    transform: rotate(360deg) scale(0.2);
    border-radius: 10%;
  }
}

@media (max-width: 768px) {
  .global-loading-panel {
    min-width: 150px;
    padding: 20px;
  }

  .global-loading-spinner {
    width: 40px;
    height: 40px;
  }
}
</style>
