package com.example.ime.panels

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.symbols.SymbolCategory
import com.example.data.symbols.SymbolEntry
import com.example.data.symbols.SymbolsCatalog
import com.example.model.KeyboardThemeSpec

class SymbolsPanelView(
    context: Context,
    private val onCommitSymbol: (String) -> Unit,
    private val onBackspace: () -> Unit,
    private val onBackToKeyboard: () -> Unit,
    private val onRequestSearchInput: (Boolean) -> Unit,
) : LinearLayout(context) {

    private var currentCategory = SymbolCategory.ARABIC
    private var theme: KeyboardThemeSpec? = null
    private val recentSymbols = LinkedHashSet<SymbolEntry>()
    private var searchQuery = ""
    private var isSearchMode = false

    private val searchBanner: TextView
    private val tabsRow: LinearLayout
    private val recyclerView: RecyclerView
    private val headerRow: LinearLayout
    private val adapter = SymbolGridAdapter()
    private var relatedPopup: PopupWindow? = null

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(252f))

        headerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
        }
        val backBtn = makeChip("⌨ رجوع") {
            setSearchMode(false)
            onBackToKeyboard()
        }
        searchBanner = TextView(context).apply {
            text = "🔍 ابحث عن رمز بالاسم..."
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
            setOnClickListener { setSearchMode(!isSearchMode) }
        }
        val delBtn = makeChip("⌫") { onBackspace() }
        headerRow.addView(backBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(34f)).apply { marginEnd = dp(6f) })
        headerRow.addView(searchBanner, LayoutParams(0, dp(34f), 1f).apply { marginEnd = dp(6f) })
        headerRow.addView(delBtn, LayoutParams(dp(46f), dp(34f)))
        addView(headerRow)

        recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 6)
            adapter = this@SymbolsPanelView.adapter
            setPadding(dp(6f), dp(2f), dp(6f), dp(2f))
            clipToPadding = false
        }
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        val tabsScroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
        tabsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(6f), dp(4f), dp(6f), dp(4f))
        }
        SymbolCategory.entries.forEach { cat ->
            val tv = TextView(context).apply {
                text = cat.titleAr
                textSize = 12f
                tag = cat
                gravity = Gravity.CENTER
                setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
                setOnClickListener {
                    setSearchMode(false)
                    currentCategory = cat
                    updateTabColors()
                    refreshGrid()
                }
            }
            tabsRow.addView(tv, LayoutParams(LayoutParams.WRAP_CONTENT, dp(34f)).apply { marginEnd = dp(4f) })
        }
        tabsScroll.addView(tabsRow)
        addView(tabsScroll)

        refreshGrid()
    }

    fun applyConfig(theme: KeyboardThemeSpec, heightPx: Int, toolbarScale: Float = 1f) {
        this.theme = theme
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
        updateTabColors()
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

    private fun setSearchMode(enabled: Boolean) {
        isSearchMode = enabled
        if (!enabled) searchQuery = ""
        updateSearchBanner()
        onRequestSearchInput(enabled)
        refreshGrid()
    }

    private fun updateSearchBanner() {
        searchBanner.text = when {
            isSearchMode && searchQuery.isEmpty() -> "🔍 اكتب اسم الرمز..."
            isSearchMode -> "🔍 $searchQuery"
            else -> "🔍 ابحث عن رمز..."
        }
    }

    private fun updateTabColors() {
        val t = theme ?: return
        for (i in 0 until tabsRow.childCount) {
            val tv = tabsRow.getChildAt(i) as? TextView ?: continue
            val active = tv.tag == currentCategory && !isSearchMode
            tv.setTextColor(if (active) t.accentKeyTextColor.toInt() else t.keyTextColor.toInt())
            tv.background = GradientDrawable().apply {
                setColor(if (active) t.accentKeyBackgroundColor.toInt() else t.specialKeyBackgroundColor.toInt())
                cornerRadius = dp(12f).toFloat()
            }
        }
    }

    private fun refreshGrid() {
        val list = if (isSearchMode && searchQuery.isNotBlank()) {
            SymbolsCatalog.search(searchQuery)
        } else if (currentCategory == SymbolCategory.RECENT) {
            recentSymbols.toList()
        } else {
            SymbolsCatalog.categories[currentCategory].orEmpty()
        }
        adapter.submit(list)
    }

    private fun showRelatedPopup(anchor: android.view.View, entry: SymbolEntry) {
        val list = entry.related
        if (list.isEmpty()) return
        val t = theme ?: return
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(6f), dp(6f), dp(6f), dp(6f))
            background = GradientDrawable().apply {
                setColor(t.popupBackgroundColor.toInt())
                cornerRadius = dp(12f).toFloat()
            }
        }
        list.forEach { sym ->
            val tv = TextView(context).apply {
                text = sym
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(t.popupTextColor.toInt())
                setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
                setOnClickListener {
                    onCommitSymbol(sym)
                    relatedPopup?.dismiss()
                }
            }
            row.addView(tv)
        }
        relatedPopup?.dismiss()
        relatedPopup = PopupWindow(row, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true).apply {
            elevation = dp(8f).toFloat()
            showAsDropDown(anchor, 0, -dp(84f))
        }
    }

    private fun makeChip(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            setOnClickListener { onClick() }
        }

    private inner class SymbolGridAdapter : RecyclerView.Adapter<SymbolGridAdapter.VH>() {
        private var items: List<SymbolEntry> = emptyList()

        fun submit(newItems: List<SymbolEntry>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                gravity = Gravity.CENTER
                textSize = 19f
            }
            tv.layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, dp(42f)).apply {
                setMargins(dp(3f), dp(3f), dp(3f), dp(3f))
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val t = theme
            holder.tv.text = item.symbol
            if (t != null) {
                holder.tv.setTextColor(t.keyTextColor.toInt())
                holder.tv.background = GradientDrawable().apply {
                    setColor(t.keyBackgroundColor.toInt())
                    cornerRadius = dp(8f).toFloat()
                }
            }
            holder.tv.setOnClickListener {
                recentSymbols.remove(item)
                val updated = listOf(item) + recentSymbols.take(23)
                recentSymbols.clear()
                recentSymbols.addAll(updated)
                onCommitSymbol(item.symbol)
            }
            holder.tv.setOnLongClickListener {
                if (item.related.isNotEmpty()) {
                    showRelatedPopup(holder.tv, item)
                    true
                } else false
            }
        }

        override fun getItemCount(): Int = items.size

        inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
}
