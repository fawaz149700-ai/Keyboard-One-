package com.example.ime.panels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.db.ClipboardItemEntity
import com.example.ime.BiometricLockManager
import com.example.model.KeyboardThemeSpec
import com.example.ui.BiometricUnlockActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardPanelView(
    context: Context,
    private val onPasteText: (String) -> Unit,
    private val onCopyText: (String) -> Unit,
    private val onTogglePin: (ClipboardItemEntity) -> Unit,
    private val onDeleteItem: (Long) -> Unit,
    private val onDeleteSelected: (List<Long>) -> Unit,
    private val onClearUnpinned: () -> Unit,
    private val onToggleGridMode: (Boolean) -> Unit,
    private val onAddQuickTemplate: () -> Unit,
    private val onBackToKeyboard: () -> Unit,
) : FrameLayout(context) {

    private var theme: KeyboardThemeSpec? = null
    private var showPinnedTab = false
    private var multiSelectMode = false
    private var isGridMode = false
    private var confirmDeleteEnabled = true
    private val selectedIds = mutableSetOf<Long>()
    private var allItems: List<ClipboardItemEntity> = emptyList()
    private var biometricLocked = false

    private val mainContent: LinearLayout
    private val topBarScroll: HorizontalScrollView
    private val backBtn: TextView
    private val recentTabBtn: TextView
    private val pinnedTabBtn: TextView
    private val viewModeBtn: TextView
    private val multiSelectBtn: TextView
    private val deleteActionBtn: TextView
    private val templateBtn: TextView
    private val feedbackBanner: TextView
    private val lockContainer: LinearLayout
    private val recyclerView: RecyclerView
    private val emptyView: TextView
    private val adapter = ClipboardAdapter()

    // In-panel modern confirmation modal (Req 3)
    private val confirmOverlay: FrameLayout
    private val confirmCard: LinearLayout
    private val confirmTitle: TextView
    private val confirmSubtitle: TextView
    private val confirmYesBtn: TextView
    private val confirmCancelBtn: TextView
    private var pendingConfirmAction: (() -> Unit)? = null

    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    private val feedbackHandler = Handler(Looper.getMainLooper())
    private var hideFeedbackRunnable: Runnable? = null

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(256f))

        mainContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        topBarScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(10f), dp(7f), dp(10f), dp(5f))
        }
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        backBtn = makePill("‹ رجوع") { onBackToKeyboard() }
        recentTabBtn = makePill("📋 الكل") {
            showPinnedTab = false
            refreshDisplay()
        }
        pinnedTabBtn = makePill("📌 المثبتة") {
            showPinnedTab = true
            refreshDisplay()
        }
        viewModeBtn = makePill("⊞ شبكة") {
            isGridMode = !isGridMode
            applyLayoutManager()
            onToggleGridMode(isGridMode)
            refreshDisplay()
        }
        multiSelectBtn = makePill("☑ تحديد") {
            multiSelectMode = !multiSelectMode
            if (!multiSelectMode) selectedIds.clear()
            refreshDisplay()
        }
        deleteActionBtn = makePill("🗑 تنظيف") {
            if (multiSelectMode && selectedIds.isNotEmpty()) {
                val idsCopy = selectedIds.toList()
                requestConfirmedDelete("سيتم حذف ${idsCopy.size} عناصر محددة من الحافظة") {
                    onDeleteSelected(idsCopy)
                    selectedIds.clear()
                    multiSelectMode = false
                    showFeedback("✓ تم حذف ${idsCopy.size} عنصر")
                }
            } else {
                requestConfirmedDelete("سيتم حذف جميع النصوص غير المثبتة من الحافظة") {
                    onClearUnpinned()
                    showFeedback("✓ تم تنظيف العناصر غير المثبتة")
                }
            }
        }
        templateBtn = makePill("⚙ قوالب") { onAddQuickTemplate() }

        topBar.addView(backBtn, lpWrap())
        topBar.addView(recentTabBtn, lpWrap())
        topBar.addView(pinnedTabBtn, lpWrap())
        topBar.addView(viewModeBtn, lpWrap())
        topBar.addView(multiSelectBtn, lpWrap())
        topBar.addView(deleteActionBtn, lpWrap())
        topBar.addView(templateBtn, lpWrap())
        topBarScroll.addView(topBar)
        mainContent.addView(topBarScroll)

        // Feedback toast pill inside panel
        feedbackBanner = TextView(context).apply {
            textSize = 11.5f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(12f), dp(5f), dp(12f), dp(5f))
            elevation = dp(2f).toFloat()
            visibility = View.GONE
        }
        mainContent.addView(
            feedbackBanner,
            LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(12f), dp(3f), dp(12f), dp(3f))
            },
        )

        // Biometric Lock Overlay
        lockContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
        }
        val lockMsg = TextView(context).apply {
            text = "🔒 الحافظة محمية بالبصمة"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12f))
        }
        val unlockBtn = TextView(context).apply {
            text = "فتح بالبصمة أو رمز الجهاز"
            textSize = 14f
            gravity = Gravity.CENTER
            elevation = dp(3f).toFloat()
            setPadding(dp(20f), dp(11f), dp(20f), dp(11f))
            setOnClickListener {
                val intent = Intent(context, BiometricUnlockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
        lockContainer.addView(lockMsg)
        lockContainer.addView(unlockBtn)
        mainContent.addView(lockContainer, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        emptyView = TextView(context).apply {
            text = "الحافظة فارغة • انسخ أي نص لحفظه هنا للأبد"
            textSize = 13.5f
            gravity = Gravity.CENTER
            setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
            visibility = View.GONE
        }
        mainContent.addView(emptyView, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ClipboardPanelView.adapter
            setPadding(dp(10f), dp(4f), dp(10f), dp(8f))
            clipToPadding = false
        }
        mainContent.addView(recyclerView, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        addView(mainContent, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Modern Confirmation Dialog Overlay centered inside panel
        confirmOverlay = FrameLayout(context).apply {
            setBackgroundColor(0x8A000000.toInt())
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            setPadding(dp(20f), dp(12f), dp(20f), dp(12f))
            setOnClickListener { dismissConfirmDialog() }
        }
        confirmCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            elevation = dp(8f).toFloat()
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
            isClickable = true
        }
        confirmTitle = TextView(context).apply {
            text = "هل أنت متأكد من الحذف؟"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4f))
        }
        confirmSubtitle = TextView(context).apply {
            textSize = 12f
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12f))
        }
        val confirmBtnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        confirmCancelBtn = TextView(context).apply {
            text = "إلغاء"
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            elevation = dp(2f).toFloat()
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            setOnClickListener { dismissConfirmDialog() }
        }
        confirmYesBtn = TextView(context).apply {
            text = "🗑 تأكيد الحذف"
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            elevation = dp(2f).toFloat()
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            setOnClickListener {
                val act = pendingConfirmAction
                dismissConfirmDialog()
                act?.invoke()
            }
        }
        confirmBtnRow.addView(
            confirmCancelBtn,
            LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8f) },
        )
        confirmBtnRow.addView(
            confirmYesBtn,
            LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f),
        )
        confirmCard.addView(confirmTitle)
        confirmCard.addView(confirmSubtitle)
        confirmCard.addView(confirmBtnRow, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        confirmOverlay.addView(
            confirmCard,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        addView(confirmOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun updateData(
        items: List<ClipboardItemEntity>,
        theme: KeyboardThemeSpec,
        requireBiometricLock: Boolean,
        gridMode: Boolean,
        confirmDelete: Boolean,
        heightPx: Int,
        toolbarScale: Float = 1f,
    ) {
        this.allItems = items
        this.theme = theme
        this.confirmDeleteEnabled = confirmDelete
        this.biometricLocked = requireBiometricLock && !BiometricLockManager.isSessionUnlocked.value
        if (this.isGridMode != gridMode) {
            this.isGridMode = gridMode
            applyLayoutManager()
        }
        val s = toolbarScale.coerceIn(0.75f, 1.35f)
        val pillH = dp(32f * s)
        val pillSp = 11.5f * s.coerceIn(0.85f, 1.25f)
        listOf(backBtn, recentTabBtn, pinnedTabBtn, viewModeBtn, multiSelectBtn, deleteActionBtn, templateBtn).forEach { btn ->
            btn.textSize = pillSp
            btn.layoutParams = (btn.layoutParams ?: lpWrap()).apply { height = pillH }
        }
        layoutParams = (layoutParams ?: LayoutParams(LayoutParams.MATCH_PARENT, heightPx)).apply {
            height = heightPx
        }
        setBackgroundColor(theme.backgroundColor.toInt())
        topBarScroll.setBackgroundColor(theme.suggestionBarBgColor.toInt())
        styleConfirmModal(theme)
        refreshDisplay()
    }

    private fun styleConfirmModal(t: KeyboardThemeSpec) {
        confirmCard.background = GradientDrawable().apply {
            setColor(t.keyBackgroundColor.toInt())
            cornerRadius = dp(18f).toFloat()
            setStroke(dp(1f), t.dividerColor.toInt())
        }
        confirmTitle.setTextColor(t.keyTextColor.toInt())
        confirmSubtitle.setTextColor((t.keyTextColor.toInt() and 0x00FFFFFF) or 0xB8000000.toInt())
        confirmCancelBtn.setTextColor(t.specialKeyTextColor.toInt())
        confirmCancelBtn.background = GradientDrawable().apply {
            setColor(t.specialKeyBackgroundColor.toInt())
            cornerRadius = dp(12f).toFloat()
        }
        confirmYesBtn.setTextColor(0xFFFFFFFF.toInt())
        confirmYesBtn.background = GradientDrawable().apply {
            setColor(0xFFC93B3B.toInt())
            cornerRadius = dp(12f).toFloat()
        }
    }

    private fun requestConfirmedDelete(previewText: String, action: () -> Unit) {
        if (!confirmDeleteEnabled) {
            action()
            return
        }
        theme?.let { styleConfirmModal(it) }
        confirmTitle.text = "هل أنت متأكد من الحذف؟"
        confirmSubtitle.text = previewText
        pendingConfirmAction = action
        confirmOverlay.visibility = View.VISIBLE
    }

    private fun dismissConfirmDialog() {
        pendingConfirmAction = null
        confirmOverlay.visibility = View.GONE
    }

    private fun applyLayoutManager() {
        recyclerView.layoutManager = if (isGridMode) {
            GridLayoutManager(context, 2)
        } else {
            LinearLayoutManager(context)
        }
        adapter.notifyDataSetChanged()
    }

    private fun showFeedback(msg: String) {
        val t = theme ?: return
        hideFeedbackRunnable?.let { feedbackHandler.removeCallbacks(it) }
        feedbackBanner.text = msg
        feedbackBanner.setTextColor(t.accentKeyTextColor.toInt())
        feedbackBanner.background = GradientDrawable().apply {
            setColor(t.accentKeyBackgroundColor.toInt())
            cornerRadius = dp(12f).toFloat()
        }
        feedbackBanner.visibility = View.VISIBLE
        val r = Runnable { feedbackBanner.visibility = View.GONE }
        hideFeedbackRunnable = r
        feedbackHandler.postDelayed(r, 1500L)
    }

    private fun refreshDisplay() {
        val t = theme ?: return
        val pinnedCount = allItems.count { it.isPinned }
        recentTabBtn.text = "📋 الكل (${allItems.size})"
        pinnedTabBtn.text = "📌 المثبتة ($pinnedCount)"
        viewModeBtn.text = if (isGridMode) "☰ قائمة" else "⊞ شبكة"

        styleTab(backBtn, active = false, t)
        styleTab(recentTabBtn, !showPinnedTab, t)
        styleTab(pinnedTabBtn, showPinnedTab, t)
        styleTab(viewModeBtn, isGridMode, t)
        styleTab(multiSelectBtn, multiSelectMode, t)
        deleteActionBtn.text = if (multiSelectMode && selectedIds.isNotEmpty()) {
            "🗑 حذف (${selectedIds.size})"
        } else {
            "🗑 تنظيف"
        }
        styleTab(deleteActionBtn, false, t)
        styleTab(templateBtn, false, t)

        if (biometricLocked) {
            lockContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.GONE
            (lockContainer.getChildAt(0) as? TextView)?.setTextColor(t.keyTextColor.toInt())
            (lockContainer.getChildAt(1) as? TextView)?.apply {
                setTextColor(t.accentKeyTextColor.toInt())
                background = GradientDrawable().apply {
                    setColor(t.accentKeyBackgroundColor.toInt())
                    cornerRadius = dp(14f).toFloat()
                }
            }
            return
        }

        lockContainer.visibility = View.GONE
        val filtered = if (showPinnedTab) allItems.filter { it.isPinned } else allItems
        if (filtered.isEmpty()) {
            emptyView.text = if (showPinnedTab) {
                "لا توجد عناصر مثبّتة بعد • اضغط تثبيت على أي عنصر"
            } else {
                "الحافظة فارغة • انسخ أي نص ليُحفظ هنا بشكل دائم"
            }
            emptyView.visibility = View.VISIBLE
            emptyView.setTextColor(t.keyTextColor.toInt())
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submit(filtered)
        }
    }

    private fun styleTab(btn: TextView, active: Boolean, t: KeyboardThemeSpec) {
        btn.setTextColor(if (active) t.accentKeyTextColor.toInt() else t.specialKeyTextColor.toInt())
        btn.elevation = dp(if (active) 2.5f else 1f).toFloat()
        btn.background = GradientDrawable().apply {
            setColor(if (active) t.accentKeyBackgroundColor.toInt() else t.specialKeyBackgroundColor.toInt())
            cornerRadius = dp(18f).toFloat()
            if (!active) setStroke(dp(0.6f), t.dividerColor.toInt())
        }
    }

    private fun makePill(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            textSize = 11.5f
            gravity = Gravity.CENTER
            includeFontPadding = false
            outlineProvider = ViewOutlineProvider.BACKGROUND
            setPadding(dp(11f), dp(5f), dp(11f), dp(5f))
            setOnClickListener { onClick() }
        }

    private fun lpWrap() = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(32f)).apply { marginEnd = dp(5f) }

    private inner class ClipboardAdapter : RecyclerView.Adapter<ClipboardAdapter.VH>() {
        private var list: List<ClipboardItemEntity> = emptyList()

        fun submit(items: List<ClipboardItemEntity>) {
            list = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val card = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                elevation = dp(3f).toFloat()
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
                setPadding(dp(11f), dp(9f), dp(11f), dp(9f))
            }
            val headerRow = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(4f))
            }
            val badgeView = TextView(parent.context).apply {
                textSize = 10f
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(7f), dp(2f), dp(7f), dp(2f))
            }
            val timeView = TextView(parent.context).apply {
                textSize = 10f
                gravity = Gravity.END
            }
            headerRow.addView(badgeView, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(6f) })
            headerRow.addView(timeView, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

            val topText = TextView(parent.context).apply {
                maxLines = 3
                textSize = 13.5f
                setLineSpacing(dp(2f).toFloat(), 1f)
            }
            val actionsRow = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(7f), 0, 0)
            }
            val btnPaste = makeActionChip("↵ لصق")
            val btnCopy = makeActionChip("⧉ نسخ")
            val btnPin = makeActionChip("📌 تثبيت")
            val btnDel = makeActionChip("🗑 حذف")

            actionsRow.addView(btnPaste, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4f) })
            actionsRow.addView(btnCopy, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4f) })
            actionsRow.addView(btnPin, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4f) })
            actionsRow.addView(btnDel, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

            card.addView(headerRow)
            card.addView(topText)
            card.addView(actionsRow)
            card.layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                val h = dp(4f)
                setMargins(h, h, h, h)
            }
            return VH(card, badgeView, timeView, topText, btnCopy, btnPaste, btnPin, btnDel)
        }

        private fun makeActionChip(label: String): TextView =
            TextView(context).apply {
                text = label
                textSize = 10.5f
                gravity = Gravity.CENTER
                maxLines = 1
                includeFontPadding = false
                elevation = dp(1.5f).toFloat()
                setPadding(dp(4f), dp(5f), dp(4f), dp(5f))
            }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            val t = theme ?: return
            val isSelected = item.id in selectedIds

            val badgeLabel = when {
                item.isPinned && !item.label.isNullOrBlank() -> "📌 ${item.label}"
                item.isPinned -> "📌 مثبّت"
                !item.label.isNullOrBlank() -> item.label
                else -> "نص محفوظ"
            }
            holder.badgeView.text = badgeLabel
            holder.badgeView.setTextColor(if (item.isPinned) t.accentKeyTextColor.toInt() else t.specialKeyTextColor.toInt())
            holder.badgeView.background = GradientDrawable().apply {
                setColor(if (item.isPinned) t.accentKeyBackgroundColor.toInt() else t.specialKeyBackgroundColor.toInt())
                cornerRadius = dp(10f).toFloat()
            }

            holder.topText.text = item.text
            holder.topText.maxLines = if (isGridMode) 2 else 3
            holder.topText.setTextColor(t.keyTextColor.toInt())
            holder.timeView.text = dateFormat.format(Date(item.timestamp))
            holder.timeView.setTextColor((t.keyTextColor.toInt() and 0x00FFFFFF) or 0x99000000.toInt())

            holder.card.background = GradientDrawable().apply {
                setColor(t.keyBackgroundColor.toInt())
                cornerRadius = dp(15f).toFloat()
                if (isSelected) {
                    setStroke(dp(2f), t.accentKeyBackgroundColor.toInt())
                } else {
                    setStroke(dp(0.8f), t.dividerColor.toInt())
                }
            }

            listOf(holder.btnCopy, holder.btnPin, holder.btnDel).forEach { b ->
                b.setTextColor(t.specialKeyTextColor.toInt())
                b.background = GradientDrawable().apply {
                    setColor(t.specialKeyBackgroundColor.toInt())
                    cornerRadius = dp(11f).toFloat()
                    setStroke(dp(0.5f), t.dividerColor.toInt())
                }
            }
            holder.btnPaste.setTextColor(t.accentKeyTextColor.toInt())
            holder.btnPaste.background = GradientDrawable().apply {
                setColor(t.accentKeyBackgroundColor.toInt())
                cornerRadius = dp(11f).toFloat()
            }
            holder.btnPin.text = if (item.isPinned) "📌 إلغاء" else "📌 تثبيت"

            holder.card.setOnClickListener {
                if (multiSelectMode) {
                    if (isSelected) selectedIds.remove(item.id) else selectedIds.add(item.id)
                    refreshDisplay()
                } else {
                    onPasteText(item.text)
                }
            }
            holder.btnCopy.setOnClickListener {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("khatt_clip", item.text))
                onCopyText(item.text)
            }
            holder.btnPaste.setOnClickListener { onPasteText(item.text) }
            holder.btnPin.setOnClickListener {
                onTogglePin(item)
                showFeedback(if (item.isPinned) "تم إلغاء التثبيت" else "📌 تم التثبيت")
            }
            holder.btnDel.setOnClickListener {
                val snippet = item.text.replace("\n", " ").take(42)
                requestConfirmedDelete("«$snippet»") {
                    onDeleteItem(item.id)
                    showFeedback("✓ تم حذف العنصر")
                }
            }
        }

        override fun getItemCount(): Int = list.size

        inner class VH(
            val card: LinearLayout,
            val badgeView: TextView,
            val timeView: TextView,
            val topText: TextView,
            val btnCopy: TextView,
            val btnPaste: TextView,
            val btnPin: TextView,
            val btnDel: TextView,
        ) : RecyclerView.ViewHolder(card)
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
}
