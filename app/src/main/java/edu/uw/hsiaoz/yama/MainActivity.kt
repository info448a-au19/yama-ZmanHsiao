package edu.uw.hsiaoz.yama

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.rview_layout.view.*
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        createNotificationChannel()
        while (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS), 1)
        }

        fab.setOnClickListener {
            val intent = Intent(this, ComposeMessageActivity::class.java)
            startActivity(intent)
        }
        showInbox()

    }

    private fun showInbox() {
        var ary: Array<String> = arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE)
        var cursor = contentResolver.query(Uri.parse("content://sms"), ary, null, null, Telephony.Sms.Inbox.DEFAULT_SORT_ORDER, null)
        var results = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            for (i in 0 until cursor.count) {
                var sdf = SimpleDateFormat("MMM d, EEE h:mm a")
                val netdate = Date(cursor.getString(2).toLong())
                var time = sdf.format(netdate)
                results.add(cursor.getString(0) + "    " + time + "\n" + cursor.getString(1) + "\n")
                cursor.moveToNext()
            }
        } else {
            throw RuntimeException("No Sms")
        }
        rview.layoutManager = LinearLayoutManager(this)
        rview.adapter = MyRecyclerViewAdapter(this, results)
        cursor.close()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("0", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}


class SmsReceiver: BroadcastReceiver() {

    companion object {
        var i = 0
    }

    override fun onReceive(context: Context, intent: Intent?) {
        var pdu = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in pdu) {
            val inte = Intent().setClass(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, inte, 0)
            val i2 = Intent().setClass(context, ComposeMessageActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i2.putExtra("contact", msg.originatingAddress)
            val p2 = PendingIntent.getActivity(context, 0, i2, 0)
            var mBuilder = NotificationCompat.Builder(context, "0")
                    .setSmallIcon(R.drawable.ic_message_black_24dp)
                    .setContentTitle(msg.originatingAddress)
                    .setContentText(msg.displayMessageBody)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setStyle(NotificationCompat.BigTextStyle())
                    .setAutoCancel(true)
                    .addAction(R.drawable.ic_message_black_24dp, "View", pendingIntent)
                    .addAction(R.drawable.ic_message_black_24dp, "Reply", p2)

            with(NotificationManagerCompat.from(context)) {
                notify(i, mBuilder.build())
            }
            i++
        }
    }
}

class MyRecyclerViewAdapter(context: Context, data: List<String>): RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder>() {

    var inflater = LayoutInflater.from(context)
    var mData = data

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = inflater.inflate(R.layout.rview_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, pos: Int) {
        holder.tView.text = mData.get(pos)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tView = view.info
    }
}