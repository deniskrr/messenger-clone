package com.deepster.messenger.message

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepster.messenger.R
import com.deepster.messenger.auth.LoginActivity
import com.deepster.messenger.auth.RegisterActivity
import com.deepster.messenger.model.ChatMessage
import com.deepster.messenger.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.latest_messaage_row.view.*
import kotlinx.android.synthetic.main.user_row_newmessage.view.*

class LatestMessagesActivity : AppCompatActivity() {

    val latestMessagesMap = HashMap<String, ChatMessage>()

    companion object {
        val TAG = "LatestMessages"
        var currentUser: User? = null

    }

    private val adapter = GroupAdapter<ViewHolder>()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)

        recyclerview_latest_messages.layoutManager = LinearLayoutManager(baseContext)
        recyclerview_latest_messages.adapter = adapter

        auth = FirebaseAuth.getInstance()

        verifyUserIsLoggedIn()

        fetchCurrentUser()

        listenForLatestMessages()

    }


    private fun refreshRecyclerView() {
        adapter.clear()
        latestMessagesMap.values.forEach {
            adapter.add(LatestMessage(it))
        }
    }

    private fun listenForLatestMessages() {
        val fromID = FirebaseAuth.getInstance().uid

        val reference = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromID")

        reference.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[p0.key!!] = chatMessage
                refreshRecyclerView()
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[p0.key!!] = chatMessage
                refreshRecyclerView()
            }

            override fun onChildRemoved(p0: DataSnapshot) {
            }
        })
    }

    class LatestMessage(val chatMessage: ChatMessage) : Item<ViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.latest_messaage_row
        }

        override fun bind(viewHolder: ViewHolder, position: Int) {
            val userId = if (chatMessage.fromID == currentUser!!.uid) {
                chatMessage.toID
            } else {
                chatMessage.fromID
            }

            val userRef = FirebaseDatabase.getInstance().getReference("/users/$userId")
            Log.d(TAG ,userRef.path.toString())

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onDataChange(p0: DataSnapshot) {
                    val user = p0.getValue(User::class.java) ?: return
                    Picasso.get().load(user.profileImageURL).into(viewHolder.itemView.profile_imageview_latest_messages)
                    viewHolder.itemView.username_textview_latest_message.text = user.username
                }
            })

            viewHolder.itemView.mesage_textview_latest_messages.text = chatMessage.text
        }
    }

    private fun fetchCurrentUser() {
        val uid = auth.uid
        val reference = FirebaseDatabase.getInstance().getReference("/users/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                currentUser = p0.getValue(User::class.java)
                Log.d(TAG, "Current user: ${currentUser?.username}")
            }
        })

    }

    private fun verifyUserIsLoggedIn() {
        val uid = auth.uid
        if (uid == null) {
            val intent = Intent(baseContext, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_new_message -> {
                val intent = Intent(baseContext, NewMessageActivity::class.java)
                startActivity(intent)
            }

            R.id.menu_sign_out -> {
                auth.signOut()
                verifyUserIsLoggedIn()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
