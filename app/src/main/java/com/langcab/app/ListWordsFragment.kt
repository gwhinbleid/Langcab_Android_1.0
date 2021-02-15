package com.langcab.app

import GsonRequest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth

class ListWordsFragment : Fragment() {

    lateinit var token: String
    lateinit var currentLanguage: String

    val hostName: String = "https://www.langcab.com/api"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Tell Fragment to retrieve Activity menu
        setHasOptionsMenu(true)

        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)?.addOnCompleteListener { task ->
            val idToken: String? = task.result?.token
            idToken?.let {
                token = idToken
                getCurrentLanguage(idToken)
            }
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_words, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        var searchView = menu?.findItem(R.id.app_bar_search)?.getActionView() as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                // your text view here
                loadWords(token, currentLanguage, newText)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(true)?.addOnCompleteListener { task ->
                    val idToken: String? = task.result?.token
                    // TODO find out if this is required
                    idToken?.let { token = idToken }

                }
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private fun getCurrentLanguage(idToken: String) {
        val queue = Volley.newRequestQueue(activity)
        val url = "${hostName}/language/last"
        val gsonRequest = GsonRequest(url,
            Request.Method.GET,
            String::class.java,
            mutableMapOf("Authorization" to idToken),
            { language ->
                currentLanguage = language
                loadWords(idToken, language, "")
            },
            { error -> println(error) })
        queue.add(gsonRequest)
    }

    private fun loadWords(idToken: String, language: String, searchQuery: String) {
        val queue = Volley.newRequestQueue(activity)
        val url = "${hostName}/word/?search=${searchQuery}&language=${language}&page=0&size=20&sort=timeCreated,DESC"
        val gsonRequest = GsonRequest(url,
            Request.Method.GET,
            Pageable::class.java,
            mutableMapOf("Authorization" to idToken),
            { response ->
                renderList(response, searchQuery)
            },
            { error -> println(error) })
        queue.add(gsonRequest)
    }

    private fun renderList(pageable: Pageable, searchQuery: String) {

        var currentWords: ArrayList<Word> = pageable.content as ArrayList<Word>
        val recyclerView: RecyclerView? = view?.findViewById(R.id.recyclerView)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        val adapter: RecyclerView.Adapter<WordAdapter.ViewHolder> = WordAdapter((currentWords))
        val mLayoutManager: LinearLayoutManager

        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = layoutManager
        recyclerView?.adapter = adapter
        mLayoutManager = LinearLayoutManager(activity)
        recyclerView?.setLayoutManager(mLayoutManager)

        registerInfinitScrollListener(recyclerView, mLayoutManager, pageable, searchQuery, adapter)

    }

    private fun registerInfinitScrollListener(
        recyclerView: RecyclerView?,
        mLayoutManager: LinearLayoutManager,
        pageable: Pageable,
        searchQuery: String,
        adapter: RecyclerView.Adapter<WordAdapter.ViewHolder>
    ) {
        var loading = true
        var pastVisiblesItems: Int
        var visibleItemCount: Int
        var totalItemCount: Int

        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var currentPage: Int = pageable.number + 1
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) { //check for scroll down
                    visibleItemCount = mLayoutManager.getChildCount()
                    totalItemCount = mLayoutManager.getItemCount()
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition()
                    if (loading) {
                        if (visibleItemCount + pastVisiblesItems >= totalItemCount && !pageable.last) {
                            loading = false

                            requestNextPage(pageable, currentPage, searchQuery, adapter)
                            currentPage ++

                            loading = true
                        }
                    }
                }
            }
        })
    }

    private fun requestNextPage(
        pageable: Pageable,
        currentPage: Int,
        searchQuery: String,
        adapter: RecyclerView.Adapter<WordAdapter.ViewHolder>
    ) {

        var currentWords: ArrayList<Word> = pageable.content as ArrayList<Word>
        var language: String = currentWords.first().language

        val queue = Volley.newRequestQueue(activity)
        val url =
            "${hostName}/word/?search=${searchQuery}&language=${language}&page=${currentPage}&size=15&sort=timeCreated,DESC"
        val gsonRequest = GsonRequest(url,
            Request.Method.GET,
            Pageable::class.java,
            mutableMapOf("Authorization" to token),
            { response ->
                response.content.forEach({
                    currentWords.add(it)
                })
                adapter.notifyDataSetChanged()
            },
            { error -> println(error) })
        queue.add(gsonRequest)
    }
}
