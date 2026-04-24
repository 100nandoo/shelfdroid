package dev.halim.shelfdroid.crash

import android.content.Context
import org.acra.config.CoreConfiguration
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory

class CrashFileSenderFactory : ReportSenderFactory {
  override fun create(context: Context, config: CoreConfiguration): ReportSender =
    CrashFileSender(context)
}
