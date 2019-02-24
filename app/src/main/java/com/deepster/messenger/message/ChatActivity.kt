package com.deepster.messenger.message

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepster.messenger.R
import com.deepster.messenger.model.ChatMessage
import com.deepster.messenger.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.message_from_chat.view.*
import kotlinx.android.synthetic.main.message_to_chat.view.*

class ChatActivity : AppCompatActivity() {

    private lateinit var toUser: User

    companion object {
        val TAG = "ChatLog"
    }

    val adapter = GroupAdapter<ViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chat_recyclerview.layoutManager = LinearLayoutManager(baseContext)
        chat_recyclerview.adapter = adapter

        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser.username


        listenForMessages()

        send_button_chat.setOnClickListener {
            Log.d(TAG, "Attempt to sent message")
            sendMessage()
        }
    }

    private fun listenForMessages() {
        val fromID = LatestMessagesActivity.currentUser?.uid
        val toID = toUser.uid
        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromID/$toID")
        reference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)

                if (chatMessage != null) {
                    Log.d(TAG, chatMessage.text)

                    if (chatMessage.fromID == FirebaseAuth.getInstance().uid) {
                        adapter.add(ChatFromItem(chatMessage.text))
                    } else {
                        adapter.add(ChatToItem(chatMessage.text, toUser))
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildRemoved(p0: DataSnapshot) {

            }
        })
    }


    private fun sendMessage() {
        val message = chat_edittext.text.toString()

        val fromID = FirebaseAuth.getInstance().uid
        val toID = toUser.uid

        if (fromID == null) {
            return
        }

        val fromReference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromID/$toID").push()
        val chatMessage = ChatMessage(fromReference.key!!, message, fromID, toID, System.currentTimeMillis())

        fromReference.setValue(chatMessage).addOnSuccessListener {
            Log.d(TAG, "Saved our chat message: ${fromReference.key}")
            chat_edittext.text.clear()
        }

        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toID/$fromID").push()

        toReference.setValue(chatMessage)

        val latestMessageFromReference = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromID/$toID")
        latestMessageFromReference.setValue(chatMessage)

        val latestMessageToReference = FirebaseDatabase.getInstance().getReference("/latest-messages/$toID/$fromID")
        latestMessageToReference.setValue(chatMessage)

    }
}

class ChatFromItem(val text: String = "") : Item<ViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.message_from_chat
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.from_textview_chat.text = text
    }

}


class ChatToItem(val text: String = "", val user: User) : Item<ViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.message_to_chat
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.to_textview_chat.text = text
        Picasso.get().load(user.profileImageURL).into(viewHolder.itemView.profile_imageview_chat)
    }

}