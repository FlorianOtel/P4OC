package dev.blazelight.p4oc.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import dev.blazelight.p4oc.core.log.AppLog

class PtyTerminalClient(
    private val context: Context,
    private val onTextChanged: () -> Unit = {},
    private val onTitleChanged: (String?) -> Unit = {},
    private val onSessionFinished: () -> Unit = {},
    private val onBellCallback: () -> Unit = {},
    private val onColorsChangedCallback: () -> Unit = {},
    private val onCursorStateChange: (Boolean) -> Unit = {},
    private val onPasteRequest: ((String) -> Unit)? = null
) : TerminalSessionClient {

    private val clipboardManager: ClipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onTextChanged(changedSession: TerminalSession) {
        onTextChanged.invoke()
    }

    override fun onTitleChanged(changedSession: TerminalSession) {
        onTitleChanged(changedSession.title)
    }

    override fun onSessionFinished(finishedSession: TerminalSession) {
        onSessionFinished.invoke()
    }

    override fun onBell(session: TerminalSession) {
        onBellCallback()
    }

    override fun onColorsChanged(session: TerminalSession) {
        onColorsChangedCallback()
    }

    override fun onTerminalCursorStateChange(state: Boolean) {
        onCursorStateChange(state)
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {
        text?.let {
            val clip = ClipData.newPlainText("Terminal", it)
            clipboardManager.setPrimaryClip(clip)
        }
    }

    @Suppress("SwallowedException")
    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pasteText = clip.getItemAt(0).coerceToText(context).toString()
                if (pasteText.isNotEmpty()) {
                    onPasteRequest?.invoke(pasteText) ?: session?.write(pasteText)
                }
            }
        } catch (e: SecurityException) {
            AppLog.w("PtyTerminalClient", "Terminal warning")
        } catch (e: Exception) {
            AppLog.e("PtyTerminalClient", "Terminal error")
        }
    }

    override fun getTerminalCursorStyle(): Int {
        return TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE
    }

    override fun logError(tag: String?, message: String?) {
        AppLog.e("PtyTerminalClient", "Terminal library error")
    }

    override fun logWarn(tag: String?, message: String?) {
        AppLog.w("PtyTerminalClient", "Terminal library warning")
    }

    override fun logInfo(tag: String?, message: String?) {
        AppLog.i("PtyTerminalClient", "Terminal library info")
    }

    override fun logDebug(tag: String?, message: String?) {
        AppLog.d("PtyTerminalClient", "Terminal library debug")
    }

    override fun logVerbose(tag: String?, message: String?) {
        AppLog.v("PtyTerminalClient", "Terminal library trace")
    }

    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
        AppLog.e(tag ?: "PtyTerminalClient", "Terminal error")
    }

    override fun logStackTrace(tag: String?, e: Exception?) {
        AppLog.e(tag ?: "PtyTerminalClient", "Terminal error")
    }
}
