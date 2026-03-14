package au.com.swagritech.opsapp.data

import android.content.Context
import au.com.swagritech.opsapp.model.QueuedFlightItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object FlightQueueStore {
    private const val PREF = "swat_flight_queue"
    private const val KEY_ITEMS = "items"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, QueuedFlightItem::class.java)
    private val adapter = moshi.adapter<List<QueuedFlightItem>>(listType)

    fun getAll(context: Context): List<QueuedFlightItem> {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_ITEMS, null)
            ?: return emptyList()
        return runCatching { adapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
    }

    fun enqueue(context: Context, item: QueuedFlightItem) {
        val updated = getAll(context).toMutableList().apply { add(item) }
        save(context, updated)
    }

    fun removeById(context: Context, localQueueId: String) {
        val updated = getAll(context).filterNot { it.localQueueId == localQueueId }
        save(context, updated)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY_ITEMS).apply()
    }

    fun count(context: Context): Int = getAll(context).size

    private fun save(context: Context, items: List<QueuedFlightItem>) {
        val json = adapter.toJson(items)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, json).apply()
    }
}
