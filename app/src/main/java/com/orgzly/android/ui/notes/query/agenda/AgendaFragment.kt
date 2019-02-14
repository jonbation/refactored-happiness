package com.orgzly.android.ui.notes.query.agenda

import android.os.Bundle
import android.view.*
import android.widget.ViewFlipper
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.notes.NoteItemTouchHelper
import com.orgzly.android.ui.notes.query.QueryFragment
import com.orgzly.android.ui.notes.query.QueryViewModel
import com.orgzly.android.ui.notes.query.QueryViewModelFactory
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils

/**
 * Displays agenda results.
 */
class AgendaFragment :
        QueryFragment(),
        OnViewHolderClickListener<AgendaItem>,
        ActionMode.Callback {

    private val item2databaseIds = hashMapOf<Long, Long>()

    private lateinit var viewFlipper: ViewFlipper

    lateinit var viewAdapter: AgendaAdapter


    override fun getAdapter(): SelectableItemAdapter {
        return viewAdapter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_query_agenda, container, false)

        viewFlipper = view.findViewById(R.id.fragment_query_agenda_view_flipper)

        setupRecyclerView(view)

        return view
    }

    private fun setupRecyclerView(view: View) {
        viewAdapter = AgendaAdapter(view.context, this)
        // TODO: viewAdapter.setHasStableIds(true)

        val layoutManager = LinearLayoutManager(context)

        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)

        val recyclerView = view.findViewById<RecyclerView>(R.id.fragment_query_agenda_recycler_view).also {
            it.layoutManager = layoutManager
            it.adapter = viewAdapter
            it.addItemDecoration(dividerItemDecoration)
        }

        val itemTouchHelper = NoteItemTouchHelper(object : NoteItemTouchHelper.Listener {
            override fun onSwipeLeft(id: Long) {
                listener?.onNoteFocusInBookRequest(id)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        val factory = QueryViewModelFactory.forQuery(dataRepository)

        viewModel = ViewModelProviders.of(this, factory).get(QueryViewModel::class.java)

        viewModel.viewState.observe(viewLifecycleOwner, Observer { state ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed load state: $state")

            viewFlipper.displayedChild = when (state) {
                QueryViewModel.ViewState.LOADING -> 0
                QueryViewModel.ViewState.LOADED -> 1
                else -> 1
            }
        })

        viewModel.notes().observe(viewLifecycleOwner, Observer { notes ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed notes: ${notes.size}")

            val items = AgendaItems.getList(notes, currentQuery, item2databaseIds)

            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Replacing data with ${items.size} agenda items")

            viewAdapter.submitList(items)

            val ids = notes.mapTo(hashSetOf()) { it.note.id }

            viewAdapter.getSelection().removeNonExistent(ids)

            viewAdapter.getSelection().setMap(item2databaseIds)

            activity?.invalidateOptionsMenu()

            actionModeListener?.updateActionModeForSelection(
                    viewAdapter.getSelection().count, this)
        })

        viewModel.refresh(currentQuery, AppPreferences.defaultPriority(context))
    }

    override fun onClick(view: View, position: Int, item: AgendaItem) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        if (item is AgendaItem.Note) {
            viewAdapter.getSelection().toggle(item.id)
            viewAdapter.notifyItemChanged(position)

            actionModeListener?.updateActionModeForSelection(
                    viewAdapter.getSelection().count, this)
        }
    }

    override fun onLongClick(view: View, position: Int, item: AgendaItem) {
//        if (item is AgendaItem.Note) {
//            val noteId = item.note.note.id
//
//            listener?.onNoteOpen(noteId)
//        }
    }

    override fun onBottomActionItemClicked(id: Int) {
        handleActionItemClick(id, actionModeListener?.actionMode, viewAdapter.getSelection())
    }

    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
        handleActionItemClick(menuItem.itemId, actionMode, viewAdapter.getSelection())

        return true
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
//        val inflater = actionMode.menuInflater
//
//        inflater.inflate(R.menu.query_cab, menu)

        sharedMainActivityViewModel.lockDrawer()

        return true
    }

    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        /* Update action mode with number of selected items. */
        actionMode.title = viewAdapter.getSelection().count.toString()

        return true
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        viewAdapter.getSelection().clear()
        viewAdapter.notifyDataSetChanged() // FIXME

        actionModeListener?.actionModeDestroyed()

        sharedMainActivityViewModel.unlockDrawer()
    }

    override fun onInflateBottomActionMode(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.bottom_action_bar_query)

        // Hide buttons that can't be used when multiple notes are selected
        listOf(
                R.id.bottom_action_bar_focus,
                R.id.bottom_action_bar_open).forEach { id ->

            toolbar.menu.findItem(id)?.isVisible = viewAdapter.getSelection().count <= 1
        }

        ActivityUtils.distributeToolbarItems(activity, toolbar)
    }

    override fun announceChangesToActivity() {
        sharedMainActivityViewModel.setFragment(
                FRAGMENT_TAG,
                getString(R.string.agenda),
                currentQuery,
                viewAdapter.getSelection().count)
    }

    companion object {
        private val TAG = AgendaFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        @JvmField
        val FRAGMENT_TAG: String = AgendaFragment::class.java.name


        @JvmStatic
        fun getInstance(query: String): QueryFragment {
            val fragment = AgendaFragment()

            val args = Bundle()
            args.putString(ARG_QUERY, query)

            fragment.arguments = args

            return fragment
        }
    }

}