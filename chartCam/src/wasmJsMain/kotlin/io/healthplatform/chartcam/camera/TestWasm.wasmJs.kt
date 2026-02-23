package io.healthplatform.chartcam.camera

private fun getVideoConstraints(mode: String): org.w3c.dom.mediacapture.MediaStreamConstraints = js("({ video: { facingMode: mode } })")
