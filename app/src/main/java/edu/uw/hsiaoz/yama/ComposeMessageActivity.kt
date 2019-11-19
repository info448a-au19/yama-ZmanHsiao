package edu.uw.hsiaoz.yama

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.telephony.SmsManager
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_compose_message.*
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.provider.Settings


class ComposeMessageActivity : AppCompatActivity() {

    val PICK_CONTACT_REQUEST = 1  // The request code
    var contactPicked = false
    val INTENTCODE = "edu.uw.intentdemo.ACTION_SMS_STATUS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_message)

        if (intent.hasExtra("contact")) {
            but.text = intent.getStringExtra("contact")
        }

        but.setOnClickListener {
            pickContact()
        }
        send.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
            } else {
                var mgr = SmsManager.getDefault()
                if (contactPicked) {
                    if (msg.text.isNotEmpty()) {
                        val intent = Intent(INTENTCODE)
                        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
                        mgr.sendTextMessage(but.text.toString(), null, msg.text.toString(), pendingIntent, null)
                        but.text = "Choose Contact"
                        msg.setText("")
                        contactPicked = false
                    }
                    else
                        AlertDialog.Builder(this).setTitle("Error").setMessage("Must Include a Message").create().show()
                } else
                    AlertDialog.Builder(this).setTitle("Error").setMessage("Must Choose Contact").create().show()
            }
        }

        val bReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if(intent!!.getAction() == INTENTCODE) {
                    if (getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(context, "Message sent!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(context, "Error sending message", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        this.registerReceiver(bReceiver, IntentFilter(INTENTCODE))
    }

    private fun pickContact() {
        Intent(Intent.ACTION_PICK, Uri.parse("content://contacts")).also { pickContactIntent ->
            pickContactIntent.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE // Show user only contacts w/ phone numbers
            startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check which request we're responding to
        if (requestCode == PICK_CONTACT_REQUEST) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                // We only need the NUMBER column, because there will be only one row in the result
                val projection: Array<String> = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

                // Get the URI that points to the selected contact
                data?.data?.also { contactUri ->
                    // Perform the query on the contact to get the NUMBER column
                    // We don't need a selection or sort order (there's only one result for this URI)
                    // CAUTION: The query() method should be called from a separate thread to avoid
                    // blocking your app's UI thread. (For simplicity of the sample, this code doesn't
                    // do that.)
                    // Consider using <code><a href="/reference/android/content/CursorLoader.html">CursorLoader</a></code> to perform the query.
                    contentResolver.query(contactUri, projection, null, null, null)?.apply {
                        moveToFirst()

                        // Retrieve the phone number from the NUMBER column
                        val column: Int = getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val number: String? = getString(column)

                        but.text = number
                        contactPicked = true
                    }
                }
            }
        }
    }
}
