# Ad SDK emulator smoke evidence — 2026-08-31

- Target: `emulator-5554`, AVD `Pixel_10_Pro_XL`, Android 17.
- Installed variant: `devDebug`.
- Instrumentation result: `OK (51 tests)`, elapsed 141.678 seconds.
- Cold-start focus after 15 seconds: `com.mckimquyen.opencal/.ui.MainActivity`.
- No `FATAL EXCEPTION` or `AndroidRuntime` crash was present in the filtered app log.

Observed runtime checkpoints from logcat:

```text
consent canRequestAds=true
onProviderInitComplete ✅ INITIALIZED (provider=AdMob)
This request is sent from a test device.
AdMobInter: ✅ Loaded gen=0
AdMobAppOpen: ✅ Loaded gen=1
AdMobRewarded: ✅ Loaded gen=0
```

The requests used Google's sample units for Interstitial, App Open and Rewarded. The screenshot
`ad-sdk-smoke-emulator.png` captures the resulting MainActivity after the consent/splash pipeline.
