import android.content.Context
import android.view.ContextMenu
import android.view.LayoutInflater // Класс для создания View из XML-разметки
import android.view.Menu
import android.view.View // Базовый класс для всех View в Android
import android.view.ViewGroup // Класс, представляющий собой контейнер для View
import android.widget.TextView // Класс для отображения текста
import androidx.recyclerview.widget.RecyclerView // Класс для работы с RecyclerView
import com.lera.myapplication.ChildrenActivity // Ваш класс Activity, содержащий данные о детях
import com.lera.myapplication.R // Ваш файл ресурсов
import java.text.SimpleDateFormat // Класс для форматирования даты
import java.util.Date
import java.util.Locale // Класс для работы с настройками региона

class ChildListAdapter(private var children: MutableList<ChildrenActivity.Child>) : RecyclerView.Adapter<ChildListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        val nameTextView: TextView = itemView.findViewById(R.id.childName)
        val birthDateTextView: TextView = itemView.findViewById(R.id.childBirthDate)


    }

    companion object {
        const val DELETE_CHILD = 1
        const val EDIT_CHILD = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val child = children[position]

        holder.nameTextView.text = child.name
        holder.birthDateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(child.birthday)

        holder.itemView.setOnLongClickListener {
            // Устанавливаем текущую позицию для удаления
            holder.itemView.context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putInt("delete_position", position).apply()
            false
        }
    }

    fun deleteChild(position: Int) {
        children.removeAt(position)
        notifyItemRemoved(position)
    }
    // Метод для получения ребенка по позиции
    fun getChildAtPosition(position: Int): ChildrenActivity.Child {
        return children[position]
    }

    // Метод для обновления данных ребенка
    fun updateChild(position: Int, newName: String, newBirthday: Date) {
        val child = children[position]
        child.name = newName
        child.birthday = newBirthday
        notifyItemChanged(position)
    }
    override fun getItemCount(): Int = children.size
}
