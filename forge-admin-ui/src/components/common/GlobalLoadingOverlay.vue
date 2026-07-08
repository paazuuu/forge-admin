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
  background: color-mix(in srgb, var(--bg-primary, #ffffff) 68%, transparent);
  backdrop-filter: blur(1px);
  pointer-events: auto;
  user-select: none;
}

.global-loading-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  min-width: 180px;
  padding: 22px 26px;
  color: var(--text-primary, #1d2129);
  background: color-mix(in srgb, var(--bg-primary, #ffffff) 92%, transparent);
  border: 1px solid color-mix(in srgb, var(--border-color, #e5e6eb) 86%, transparent);
  border-radius: 8px;
  box-shadow: 0 18px 42px color-mix(in srgb, #000000 12%, transparent);
}

.global-loading-spinner {
  --loader-color: var(--primary-color, #165dff);

  width: 34px;
  height: 34px;
  border: 3px solid color-mix(in srgb, var(--loader-color) 18%, transparent);
  border-top-color: var(--loader-color);
  border-radius: 50%;
  animation: global-loading-rotate 0.8s linear infinite;
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
  background: color-mix(in srgb, var(--bg-primary, #0f172a) 74%, transparent);
}

:global(.dark) .global-loading-panel {
  background: color-mix(in srgb, var(--bg-primary, #0f172a) 92%, transparent);
  border-color: color-mix(in srgb, var(--border-color, #334155) 70%, transparent);
  box-shadow: 0 18px 42px color-mix(in srgb, #000000 34%, transparent);
}

:global(.dark) .global-loading-text {
  color: var(--text-secondary, #c9d1d9);
}

@keyframes global-loading-rotate {
  100% {
    transform: rotate(360deg);
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
