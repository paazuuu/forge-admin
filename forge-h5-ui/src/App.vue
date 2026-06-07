<script>
function hideNativeTabBar() {
  if (typeof uni === 'undefined' || typeof uni.hideTabBar !== 'function') {
    return
  }
  uni.hideTabBar({
    animation: false,
    fail: () => {},
  })
}

export default {
  onLaunch: function () {
    hideNativeTabBar()
    console.log('App Launch')
  },
  onShow: function () {
    hideNativeTabBar()
    console.log('App Show')
  },
  onHide: function () {
    console.log('App Hide')
  },
}
</script>

<style lang="scss">
@import "uview-plus/index.scss";
@import "@/styles/theme.css";
@import "@/styles/global.css";
@import "@/styles/dialog.css";
@import "@/styles/notify.css";

/*每个页面公共css */
</style>
