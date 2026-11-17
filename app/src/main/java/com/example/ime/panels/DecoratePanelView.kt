package com.example.ime.panels

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.db.FavoriteDecorationEntity
import com.example.data.decorate.DecorationFrame
import com.example.data.decorate.DecorationStyle
import com.example.data.decorate.TextDecorator
import com.example.model.KeyboardThemeSpec

class DecoratePanelView(
    context: Context,
    private val onReplaceText: (String) -> Unit,
    private val onInsertText: (String) -> Unit,
    private val onToggleFavorite: (DecorationStyle, DecorationFrame) -> Unit,
    private val onApplyFawaz: () -> Unit,
    private val onToggleRealtimeDecoration: (Boolean) -> Unit,
    private val onBackToKeyboard: () -> Unit,
) : LinearLayout(context) {

    private var theme: KeyboardThemeSpec? = null
    private var sourceText: String = "مرحبا"
    private var selectedFrame: DecorationFrame = TextDecorator.frames.first()
    private var filterTab: Int = 0 // 0=all, 1=arabic, 2=english, 3=favorites
    private var favorites: List<FavoriteDecorationEntity> = emptyList()
    private var isRealtimeEnabled: Boolean = true

    private val fawazBanner: LinearLayout
    private val fawazTitle: TextView
    private val fawazPreview: TextView
    private val fawazBtn: TextView
    private val framesRow: LinearLayout
    private val filterRow: LinearLayout
    private val recyclerView: RecyclerView
    private val adapter = DecorateAdapter()

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(256f))

        // Top controls
        filterRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f), dp(6f), dp(8f), dp(4f))
        }
        val backBtn = makeChip("⌨ رجوع") { onBackToKeyboard() }
        val tabAll = makeChip("الكل") { filterTab = 0; refreshList() }
        val tabAr = makeChip("عربي") { filterTab = 1; refreshList() }
        val tabEn = makeChip("إنجليزي") { filterTab = 2; refreshList() }
        val tabFav = makeChip("★ المفضلة") { filterTab = 3; refreshList() }

        filterRow.addView(backBtn, lpWrap())
        filterRow.addView(tabAll, lpWrap())
        filterRow.addView(tabAr, lpWrap())
        filterRow.addView(tabEn, lpWrap())
        filterRow.addView(tabFav, lpWrap())
        addView(filterRow)

        // Fawaz Real-Time Decoration + One-Tap Apply Card
        fawazBanner = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
        }
        val fawazTextCol = LinearLayout(context).apply { orientation = VERTICAL }
        fawazTitle = TextView(context).apply {
            text = "⚡ الزخرفة الفورية أثناء الكتابة (فواز)"
            textSize = 12f
        }
        fawazPreview = TextView(context).apply {
            text = "ــــ͡ــٰ̡ـ (تُدرج تلقائياً وسط الكلمة لـ ك ي س ش ق ج خ ع غ)"
            textSize = 11f
            maxLines = 1
        }
        fawazTextCol.addView(fawazTitle)
        fawazTextCol.addView(fawazPreview)
        fawazBtn = TextView(context).apply {
            text = "⚡ فوري: مفعّل"
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(11f), dp(6f), dp(11f), dp(6f))
            setOnClickListener {
                onToggleRealtimeDecoration(!isRealtimeEnabled)
            }
        }
        fawazBanner.addView(fawazTextCol, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        fawazBanner.addView(fawazBtn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8f) })
        fawazBanner.setOnClickListener { onApplyFawaz() }
        addView(
            fawazBanner,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(8f), dp(2f), dp(8f), dp(4f))
            },
        )

        // Frame selector bar
        val framesScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(8f), dp(2f), dp(8f), dp(4f))
        }
        framesRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        TextDecorator.frames.forEach { frame ->
            val tv = TextView(context).apply {
                text = if (frame.id == "none") "بدون إطار" else "${frame.prefix}نص${frame.suffix}"
                textSize = 11.5f
                tag = frame
                gravity = Gravity.CENTER
                setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
                setOnClickListener {
                    selectedFrame = frame
                    updateFrameChips()
                    refreshList()
                }
            }
            framesRow.addView(tv, lpWrap())
        }
        framesScroll.addView(framesRow)
        addView(framesScroll)

        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@DecoratePanelView.adapter
            setPadding(dp(8f), dp(2f), dp(8f), dp(6f))
            clipToPadding = false
        }
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun updateData(
        inputText: String,
        favorites: List<FavoriteDecorationEntity>,
        theme: KeyboardThemeSpec,
        realTimeEnabled: Boolean,
        heightPx: Int,
        toolbarScale: Float = 1f,
    ) {
        this.sourceText = inputText.ifBlank { "كيبورد خط • Khatt" }
        this.favorites = favorites
        this.theme = theme
        this.isRealtimeEnabled = realTimeEnabled
        val s = toolbarScale.coerceIn(0.75f, 1.35f)
        val pillH = dp(32f * s)
        for (i in 0 until filterRow.childCount) {
            val tv = filterRow.getChildAt(i) as? TextView ?: continue
            tv.textSize = 12f * s.coerceIn(0.85f, 1.2f)
            tv.layoutParams = (tv.layoutParams as LayoutParams).apply { height = pillH }
        }
        layoutParams = (layoutParams ?: LayoutParams(LayoutParams.MATCH_PARENT, heightPx)).apply {
            height = heightPx
        }
        setBackgroundColor(theme.backgroundColor.toInt())
        fawazBanner.background = GradientDrawable().apply {
            setColor(theme.keyBackgroundColor.toInt())
            cornerRadius = dp(12f).toFloat()
            setStroke(dp(1f), theme.accentKeyBackgroundColor.toInt())
        }
        fawazTitle.setTextColor(theme.keyTextColor.toInt())
        val sampleFawaz = TextDecorator.decorateFawaz(this.sourceText)
        fawazPreview.text = sampleFawaz
        fawazPreview.setTextColor((theme.keyTextColor.toInt() and 0x00FFFFFF) or 0xCC000000.toInt())
        fawazBtn.text = if (realTimeEnabled) "⚡ فوري: مفعّل" else "⚡ فوري: متوقف"
        fawazBtn.setTextColor(if (realTimeEnabled) theme.accentKeyTextColor.toInt() else theme.specialKeyTextColor.toInt())
        fawazBtn.background = GradientDrawable().apply {
            setColor(if (realTimeEnabled) theme.accentKeyBackgroundColor.toInt() else theme.specialKeyBackgroundColor.toInt())
            cornerRadius = dp(12f).toFloat()
        }
        updateFilterChips()
        updateFrameChips()
        refreshList()
    }

    private fun updateFilterChips() {
        val t = theme ?: return
        for (i in 0 until filterRow.childCount) {
            val tv = filterRow.getChildAt(i) as? TextView ?: continue
            val active = (i - 1) == filterTab
            tv.setTextColor(if (active) t.accentKeyTextColor.toInt() else t.specialKeyTextColor.toInt())
            tv.background = GradientDrawable().apply {
                setColor(if (active) t.accentKeyBackgroundColor.toInt() else t.specialKeyBackgroundColor.toInt())
                cornerRadius = dp(10f).toFloat()
            }
        }
    }

    private fun updateFrameChips() {
        val t = theme ?: return
        for (i in 0 until framesRow.childCount) {
            val tv = framesRow.getChildAt(i) as? TextView ?: continue
            val frame = tv.tag as? DecorationFrame
            val active = frame?.id == selectedFrame.id
            tv.setTextColor(if (active) t.accentKeyTextColor.toInt() else t.keyTextColor.toInt())
            tv.background = GradientDrawable().apply {
                setColor(if (active) t.accentKeyBackgroundColor.toInt() else t.keyBackgroundColor.toInt())
                cornerRadius = dp(12f).toFloat()
            }
        }
    }

    private fun refreshList() {
        updateFilterChips()
        val items = when (filterTab) {
            1 -> TextDecorator.styles.filter { it.isArabicStyle }.map { it to selectedFrame }
            2 -> TextDecorator.styles.filter { !it.isArabicStyle }.map { it to selectedFrame }
            3 -> favorites.mapNotNull { fav ->
                val style = TextDecorator.styles.firstOrNull { it.id == fav.styleId } ?: return@mapNotNull null
                val frame = DecorationFrame("fav_${fav.id}", fav.title, fav.prefix, fav.suffix)
                style to frame
            }
            else -> TextDecorator.styles.map { it to selectedFrame }
        }
        adapter.submit(items)
    }

    private fun makeChip(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(10f), dp(5f), dp(10f), dp(5f))
            setOnClickListener { onClick() }
        }

    private fun lpWrap() = LayoutParams(LayoutParams.WRAP_CONTENT, dp(32f)).apply { marginEnd = dp(4f) }

    private inner class DecorateAdapter : RecyclerView.Adapter<DecorateAdapter.VH>() {
        private var list: List<Pair<DecorationStyle, DecorationFrame>> = emptyList()

        fun submit(items: List<Pair<DecorationStyle, DecorationFrame>>) {
            list = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val row = LinearLayout(parent.context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10f), dp(8f), dp(10f), dp(8f))
            }
            val textCol = LinearLayout(parent.context).apply { orientation = VERTICAL }
            val title = TextView(parent.context).apply { textSize = 11f }
            val preview = TextView(parent.context).apply { textSize = 15f; maxLines = 2 }
            textCol.addView(title)
            textCol.addView(preview)

            val starBtn = TextView(parent.context).apply { text = "☆"; textSize = 16f; setPadding(dp(8f), dp(4f), dp(8f), dp(4f)) }
            val replaceBtn = TextView(parent.context).apply { text = "استبدال"; textSize = 11.5f; setPadding(dp(10f), dp(5f), dp(10f), dp(5f)) }

            row.addView(textCol, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            row.addView(starBtn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(6f) })
            row.addView(replaceBtn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            row.layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(3f), 0, dp(3f))
            }
            return VH(row, title, preview, starBtn, replaceBtn)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (style, frame) = list[position]
            val t = theme ?: return
            val decorated = TextDecorator.decorate(sourceText, style, frame)
            val isFav = favorites.any { it.styleId == style.id && it.prefix == frame.prefix && it.suffix == frame.suffix }

            holder.title.text = style.nameAr
            holder.title.setTextColor((t.keyTextColor.toInt() and 0x00FFFFFF) or 0x99000000.toInt())
            holder.preview.text = decorated
            holder.preview.setTextColor(t.keyTextColor.toInt())
            holder.row.background = GradientDrawable().apply {
                setColor(t.keyBackgroundColor.toInt())
                cornerRadius = dp(10f).toFloat()
            }
            holder.starBtn.text = if (isFav) "★" else "☆"
            holder.starBtn.setTextColor(t.accentKeyBackgroundColor.toInt())
            holder.replaceBtn.setTextColor(t.accentKeyTextColor.toInt())
            holder.replaceBtn.background = GradientDrawable().apply {
                setColor(t.accentKeyBackgroundColor.toInt())
                cornerRadius = dp(8f).toFloat()
            }

            holder.row.setOnClickListener { onInsertText(decorated) }
            holder.replaceBtn.setOnClickListener { onReplaceText(decorated) }
            holder.starBtn.setOnClickListener { onToggleFavorite(style, frame) }
        }

        override fun getItemCount(): Int = list.size

        inner class VH(
            val row: LinearLayout,
            val title: TextView,
            val preview: TextView,
            val starBtn: TextView,
            val replaceBtn: TextView,
        ) : RecyclerView.ViewHolder(row)
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
}
