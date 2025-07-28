package dev.halim.shelfdroid.core

data class Device(
  val manufacturer: String,
  val model: String,
  val osVersion: String,
  val sdkVersion: Int,
  val clientName: String,
  val clientVersion: String,
  val mediaPlayer: String,
)
