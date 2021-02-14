package com.langcab.app

import GsonRequest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.google.firebase.auth.FirebaseAuth

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ListWordsFragment : Fragment() {

    lateinit var token: String

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
                loadWords(idToken, "")
            }
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_words, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        var searchView = menu?.findItem(R.id.app_bar_search)?.getActionView() as SearchView
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String):Boolean {
                // your text view here
                loadWords(token, newText)
                return true
            }
            override fun onQueryTextSubmit(query: String):Boolean {
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
                    idToken?.let { loadWords(idToken, "") }

                }
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private fun getCurrentLanguage() {
        // TODO implement API call to get lang
    }

    private fun loadWords(idToken: String, searchQuery: String) {
        println("LoadWords with query: " + searchQuery)
        val cache = DiskBasedCache(activity?.cacheDir, 1024 * 1024) // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        val network = BasicNetwork(HurlStack())

        // Instantiate the RequestQueue with the cache and network. Start the queue.
        val requestQueue = RequestQueue(cache, network).apply {
            start()
        }

        val hostName = "https://www.langcab.com/api/word"
        val url = "${hostName}/?search=${searchQuery}&language=Chinese&page=0&size=10&sort=timeCreated,DESC"
        val gsonRequest = GsonRequest(url, Pageable::class.java, mutableMapOf("Authorization" to idToken),
            { response ->
                renderList(response)
            },
            { error ->  println(error) })
        requestQueue.add(gsonRequest)
    }

    private fun renderList(pageable: Pageable) {
        val recyclerView: RecyclerView? = view?.findViewById(R.id.recyclerView)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        val adapter: RecyclerView.Adapter<WordAdapter.ViewHolder> = WordAdapter((pageable.content))
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = layoutManager
        recyclerView?.adapter = adapter
    }
}