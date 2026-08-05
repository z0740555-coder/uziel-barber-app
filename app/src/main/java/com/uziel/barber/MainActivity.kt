package com.uziel.barber

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var editCustomerName: EditText
    private lateinit var txtSelectedDateTime: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtAppointmentsList: TextView

    private val calendar = Calendar.getInstance()
    private var dateChosen = false
    private var timeChosen = false

    // Working hours
    private val startHour = 9
    private val endHour = 19
    private val slotMinutes = 30

    private val prefsName = "uziel_barber_prefs"
    private val appointmentsKey = "appointments"
    private val reminderMinutesBefore = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editCustomerName = findViewById(R.id.editCustomerName)
        txtSelectedDateTime = findViewById(R.id.txtSelectedDateTime)
        txtStatus = findViewById(R.id.txtStatus)
        txtAppointmentsList = findViewById(R.id.txtAppointmentsList)

        val btnPickDate: Button = findViewById(R.id.btnPickDate)
        val btnPickTime: Button = findViewById(R.id.btnPickTime)
        val btnBook: Button = findViewById(R.id.btnBook)

        btnPickDate.setOnClickListener { showDatePicker() }
        btnPickTime.setOnClickListener { showTimePicker() }
        btnBook.setOnClickListener { bookAppointment() }

        ReminderReceiver.createChannel(this)
        requestNotificationPermissionIfNeeded()

        refreshAppointmentsList()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }
    }

    private fun scheduleReminder(appointmentCal: Calendar, customerName: String) {
        val reminderCal = appointmentCal.clone() as Calendar
        reminderCal.add(Calendar.MINUTE, -reminderMinutesBefore)

        if (reminderCal.timeInMillis <= System.currentTimeMillis()) {
            return
        }

        val timeText = String.format(
            "%02d:%02d",
            appointmentCal.get(Calendar.HOUR_OF_DAY),
            appointmentCal.get(Calendar.MINUTE)
        )

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("customerName", customerName)
            putExtra("timeText", timeText)
        }

        val requestCode = (appointmentCal.timeInMillis % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderCal.timeInMillis,
            pendingIntent
        )
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            calendar.set(Calendar.YEAR, y)
            calendar.set(Calendar.MONTH, m)
            calendar.set(Calendar.DAY_OF_MONTH, d)
            dateChosen = true
            updateSelectedDateTimeLabel()
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            calendar.set(Calendar.HOUR_OF_DAY, h)
            calendar.set(Calendar.MINUTE, m)
            calendar.set(Calendar.SECOND, 0)
            timeChosen = true
            updateSelectedDateTimeLabel()
        }, hour, minute, true).show()
    }

    private fun updateSelectedDateTimeLabel() {
        if (dateChosen && timeChosen) {
            txtSelectedDateTime.text = formatDateTime(calendar)
        }
    }

    private fun formatDateTime(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val mo = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val mi = cal.get(Calendar.MINUTE)
        return String.format("%02d/%02d/%04d %02d:%02d", d, mo, y, h, mi)
    }

    private fun dateKey(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val mo = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", y, mo, d)
    }

    private fun timeKey(cal: Calendar): String {
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val mi = cal.get(Calendar.MINUTE)
        return String.format("%02d:%02d", h, mi)
    }

    private fun loadAppointments(): MutableList<String> {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val raw = prefs.getString(appointmentsKey, "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return raw.split(";;").filter { it.isNotBlank() }.toMutableList()
    }

    private fun saveAppointments(list: List<String>) {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        prefs.edit().putString(appointmentsKey, list.joinToString(";;")).apply()
    }

    // Each stored entry format: "yyyy-MM-dd|HH:mm|customerName"
    private fun bookAppointment() {
        val name = editCustomerName.text.toString().trim()
        if (name.isEmpty()) {
            txtStatus.text = "נא להזין שם לקוח"
            return
        }
        if (!dateChosen || !timeChosen) {
            txtStatus.text = "נא לבחור תאריך ושעה"
            return
        }

        val appointments = loadAppointments()
        val chosenDateKey = dateKey(calendar)
        val chosenTimeKey = timeKey(calendar)

        val takenTimes = appointments
            .mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size == 3 && parts[0] == chosenDateKey) parts[1] else null
            }
            .toSet()

        if (!takenTimes.contains(chosenTimeKey)) {
            appointments.add("$chosenDateKey|$chosenTimeKey|$name")
            saveAppointments(appointments)
            scheduleReminder(calendar, name)
            txtStatus.text = "התור נקבע בהצלחה ל-${formatDateTime(calendar)} עבור $name"
            refreshAppointmentsList()
            return
        }

        val nextFree = findNextFreeSlot(chosenTimeKey, takenTimes)
        if (nextFree == null) {
            txtStatus.text = "השעה הזו תפוסה, ואין תורים פנויים נוספים באותו יום. נסה יום אחר."
        } else {
            appointments.add("$chosenDateKey|$nextFree|$name")
            saveAppointments(appointments)
            calendar.set(Calendar.HOUR_OF_DAY, nextFree.substring(0, 2).toInt())
            calendar.set(Calendar.MINUTE, nextFree.substring(3, 5).toInt())
            txtSelectedDateTime.text = formatDateTime(calendar)
            scheduleReminder(calendar, name)
            txtStatus.text = "השעה שביקשת תפוסה. נקבע לך אוטומטית התור הפנוי הבא: $nextFree ביום ${dayLabel()}"
            refreshAppointmentsList()
        }
    }

    private fun dayLabel(): String {
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        val mo = calendar.get(Calendar.MONTH) + 1
        val y = calendar.get(Calendar.YEAR)
        return String.format("%02d/%02d/%04d", d, mo, y)
    }

    private fun findNextFreeSlot(fromTime: String, takenTimes: Set<String>): String? {
        val fromHour = fromTime.substring(0, 2).toInt()
        val fromMinute = fromTime.substring(3, 5).toInt()

        var totalMinutes = fromHour * 60 + fromMinute + slotMinutes
        val endMinutes = endHour * 60

        while (totalMinutes < endMinutes) {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            val candidate = String.format("%02d:%02d", h, m)
            if (!takenTimes.contains(candidate)) {
                return candidate
            }
            totalMinutes += slotMinutes
        }
        return null
    }

    private fun refreshAppointmentsList() {
        val appointments = loadAppointments()
        if (appointments.isEmpty()) {
            txtAppointmentsList.text = "אין תורים קבועים עדיין"
            return
        }
        val sorted = appointments.sortedWith(compareBy(
            { it.split("|")[0] },
            { it.split("|")[1] }
        ))
        val sb = StringBuilder()
        for (entry in sorted) {
            val parts = entry.split("|")
            if (parts.size == 3) {
                sb.append("${parts[0]}  ${parts[1]}  -  ${parts[2]}\n")
            }
        }
        txtAppointmentsList.text = sb.toString()
    }
}


 

     


 
