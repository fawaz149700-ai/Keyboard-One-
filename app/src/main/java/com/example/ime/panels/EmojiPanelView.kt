package com.example.ime.panels

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.emoji.EmojiCatalog
import com.example.data.emoji.EmojiCategory
import com.example.data.emoji.EmojiEntry
import com.example.model.EmojiRenderStyle
import com.example.model.KeyboardThemeSpec

class EmojiPanelView(
    context: Context,
    private val onEmojiCommit: (String) -> Unit,
    private val onBackspace: () -> Unit,
    private val onBackToKeyboard: () -> Unit,
    private val onRequestSearchInput: (Boolean) -> Unit,
) : LinearLayout(context) {

    private var currentCategory = EmojiCategory.FACES
    private var selectedSkinTone = ""
    private var renderStyle = EmojiRenderStyle.NOTO_ANDROID
    private var theme: KeyboardThemeSpec? = null
    private val recentEmojis = LinkedHashSet<String>(EmojiCatalog.defaultQuickEmojis.take(8))
    private var searchQuery = ""
    private var isSearchMode = false

    private val headerRow: LinearLayout
    private val searchBanner: TextView
    private val skinToneBar: HorizontalScrollView
    private val categoryTabsContainer: LinearLayout
    private val recyclerView: RecyclerView
    private val adapter = EmojiGridAdapter()

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(248f))

        // Top control bar: Back, Search toggle, Skin tone pill, Backspace
        headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
        }

        val backBtn = makeActionChip("⌨ رجوع") {
            setSearchMode(false)
            onBackToKeyboard()
        }

        searchBanner = TextView(context).apply {
            text = "🔍 ابحث بالاسم..."
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
            setOnClickListener {
                setSearchMode(!isSearchMode)
            }
        }

        val delBtn = makeActionChip("⌫") { onBackspace() }

        headerRow.addView(backBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(34f)).apply { marginEnd = dp(6f) })
        headerRow.addView(searchBanner, LayoutParams(0, dp(34f), 1f).apply { marginEnd = dp(6f) })
        headerRow.addView(delBtn, LayoutParams(dp(46f), dp(34f)))
        addView(headerRow)

        // Skin tone quick bar
        skinToneBar = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(8f), dp(2f), dp(8f), dp(4f))
        }
        val toneRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        val toneSamples = listOf("👋" to "", "👋🏻" to "🏻", "👋🏼" to "🏼", "👋🏽" to "🏽", "👋🏾" to "🏾", "👋🏿" to "🏿")
        toneSamples.forEach { (sample, tone) ->
            val btn = TextView(context).apply {
                text = sample
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(dp(10f), dp(2f), dp(10f), dp(2f))
                setOnClickListener {
                    selectedSkinTone = tone
                    refreshGrid()
                }
            }
            toneRow.addView(btn)
        }
        skinToneBar.addView(toneRow)
        addView(skinToneBar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Grid
        recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 7)
            adapter = this@EmojiPanelView.adapter
            setPadding(dp(6f), dp(2f), dp(6f), dp(2f))
            clipToPadding = false
        }
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        // Category tabs at bottom
        val tabsScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
        }
        categoryTabsContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(6f), dp(4f), dp(6f), dp(4f))
        }
        EmojiCategory.entries.forEach { cat ->
            val tab = TextView(context).apply {
                text = "${cat.icon} ${cat.titleAr}"
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
                tag = cat
                setOnClickListener {
                    setSearchMode(false)
                    currentCategory = cat
                    updateTabSelection()
                    refreshGrid()
                }
            }
            categoryTabsContainer.addView(tab, LayoutParams(LayoutParams.WRAP_CONTENT, dp(34f)).apply { marginEnd = dp(4f) })
        }
        tabsScroll.addView(categoryTabsContainer)
        addView(tabsScroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        refreshGrid()
    }

    fun applyConfig(theme: KeyboardThemeSpec, style: EmojiRenderStyle, heightPx: Int, toolbarScale: Float = 1f) {
        this.theme = theme
        this.renderStyle = style
        val s = toolbarScale.coerceIn(0.75f, 1.35f)
        val headerH = dp(34f * s)
        for (i in 0 until headerRow.childCount) {
            val v = headerRow.getChildAt(i)
            if (v is TextView) {
                v.textSize = 13f * s.coerceIn(0.85f, 1.2f)
                v.layoutParams = (v.layoutParams as LayoutParams).apply { height = headerH }
            }
        }
        layoutParams = (layoutParams ?: LayoutParams(LayoutParams.MATCH_PARENT, heightPx)).apply {
            height = heightPx
        }
        setBackgroundColor(theme.backgroundColor.toInt())
        searchBanner.setTextColor(theme.keyTextColor.toInt())
        searchBanner.background = GradientDrawable().apply {
            setColor(theme.keyBackgroundColor.toInt())
            cornerRadius = dp(16f).toFloat()
        }
        for (i in 0 until headerRow.childCount) {
            val v = headerRow.getChildAt(i)
            if (v is TextView && v !== searchBanner) {
                v.setTextColor(theme.specialKeyTextColor.toInt())
                v.background = GradientDrawable().apply {
                    setColor(theme.specialKeyBackgroundColor.toInt())
                    cornerRadius = dp(10f).toFloat()
                }
            }
        }
        updateTabSelection()
        adapter.notifyDataSetChanged()
    }

    fun appendSearchChar(ch: String) {
        if (!isSearchMode) return
        searchQuery += ch
        updateSearchBanner()
        refreshGrid()
    }

    fun backspaceSearchChar() {
        if (!isSearchMode) return
        if (searchQuery.isNotEmpty()) {
            searchQuery = searchQuery.dropLast(1)
            updateSearchBanner()
            refreshGrid()
        }
    }

    fun getRecentSnapshot(): List<String> = recentEmojis.toList()

    private fun setSearchMode(enabled: Boolean) {
        isSearchMode = enabled
        if (!enabled) searchQuery = ""
        updateSearchBanner()
        onRequestSearchInput(enabled)
        refreshGrid()
    }

    private fun updateSearchBanner() {
        searchBanner.text = when {
            isSearchMode && searchQuery.isEmpty() -> "🔍 اكتب بالكيبورد للبحث (أو اضغط لإلغاء)..."
            isSearchMode -> "🔍 $searchQuery"
            else -> "🔍 ابحث عن إيموجي..."
        }
    }

    private fun updateTabSelection() {
        val t = theme ?: return
        for (i in 0 until categoryTabsContainer.childCount) {
            val tv = categoryTabsContainer.getChildAt(i) as? TextView ?: continue
            val cat = tv.tag as? EmojiCategory
            val active = cat == currentCategory && !isSearchMode
            tv.setTextColor(if (active) t.accentKeyTextColor.toInt() else t.keyTextColor.toInt())
            tv.background = GradientDrawable().apply {
                setColor(if (active) t.accentKeyBackgroundColor.toInt() else t.specialKeyBackgroundColor.toInt())
                cornerRadius = dp(12f).toFloat()
            }
        }
    }

    private fun refreshGrid() {
        val items: List<String> = if (isSearchMode && searchQuery.isNotBlank()) {
            EmojiCatalog.search(searchQuery).map { entry ->
                if (entry.supportsSkinTone) EmojiCatalog.applySkinTone(entry.emoji, selectedSkinTone) else entry.emoji
            }
        } else {
            when (currentCategory) {
                EmojiCategory.RECENT -> recentEmojis.toList()
                EmojiCategory.STICKERS -> EmojiCatalog.stickersAndKaomoji
                else -> EmojiCatalog.entriesByCategory[currentCategory].orEmpty().map { entry: EmojiEntry ->
                    if (entry.supportsSkinTone) EmojiCatalog.applySkinTone(entry.emoji, selectedSkinTone) else entry.emoji
                }
            }
        }
        val span = if (currentCategory == EmojiCategory.STICKERS && !isSearchMode) 2 else 7
        (recyclerView.layoutManager as? GridLayoutManager)?.spanCount = span
        adapter.submit(items, isStickerMode = span == 2)
    }

    private fun makeActionChip(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            setOnClickListener { onClick() }
        }

    private inner class EmojiGridAdapter : RecyclerView.Adapter<EmojiGridAdapter.VH>() {
        private var items: List<String> = emptyList()
        private var stickerMode: Boolean = false

        fun submit(newItems: List<String>, isStickerMode: Boolean) {
            items = newItems
            stickerMode = isStickerMode
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val container = FrameLayout(parent.context)
            val tv = TextView(parent.context).apply {
                gravity = Gravity.CENTER
            }
            container.addView(tv, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            return VH(container, tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val value = items[position]
            val t = theme
            holder.tv.text = value
            holder.tv.textSize = if (stickerMode) 14f else 24f
            if (t != null) {
                holder.tv.setTextColor(t.keyTextColor.toInt())
                // Apple vs Noto distinct visual badge framing
                if (renderStyle == EmojiRenderStyle.APPLE_IOS) {
                    holder.root.background = GradientDrawable().apply {
                        setColor(if (t.isDark) 0x22FFFFFF else 0x33FFFFFF)
                        cornerRadius = dp(10f).toFloat()
                        setStroke(1, if (t.isDark) 0x28FFFFFF else 0x18000000)
                    }
                } else {
                    holder.root.background = null
                }
            }
            val heightPx = if (stickerMode) dp(44f) else dp(40f)
            holder.root.layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, heightPx).apply {
                setMargins(dp(2f), dp(2f), dp(2f), dp(2f))
            }
            holder.root.setOnClickListener {
                recentEmojis.remove(value)
                val updated = listOf(value) + recentEmojis.take(23)
                recentEmojis.clear()
                recentEmojis.addAll(updated)
                onEmojiCommit(value)
            }
        }

        override fun getItemCount(): Int = items.size

        inner class VH(val root: FrameLayout, val tv: TextView) : RecyclerView.ViewHolder(root)
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
}
