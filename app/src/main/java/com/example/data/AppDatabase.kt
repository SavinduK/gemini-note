package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stickydesk_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            val noteDao = database.noteDao()
            val taskDao = database.taskDao()
            val transactionDao = database.transactionDao()
            val chatDao = database.chatDao()

            // 1. Categories
            categoryDao.insertCategory(CategoryEntity(name = "Projects", colorHex = "#FFCDD2", iconName = "folder", isDefault = true))
            categoryDao.insertCategory(CategoryEntity(name = "Travel", colorHex = "#FFF9C4", iconName = "flight", isDefault = true))
            categoryDao.insertCategory(CategoryEntity(name = "Tech", colorHex = "#E1F5FE", iconName = "code", isDefault = true))
            categoryDao.insertCategory(CategoryEntity(name = "Clinical", colorHex = "#E8F5E9", iconName = "medical_services", isDefault = true))
            categoryDao.insertCategory(CategoryEntity(name = "Other", colorHex = "#FFE0B2", iconName = "note", isDefault = true))

            // 2. Initial Sticky Notes inspired by the reference screenshot
            // Pastel Yellow - Social Media
            noteDao.insertNote(
                NoteEntity(
                    title = "Social Media",
                    content = "- Plan social content\n- Build content calendar\n- Plan promotion and distribution",
                    category = "Projects",
                    colorHex = "#FFF9C4", // Pastel Yellow
                    tags = "Social, Marketing",
                    isPinned = true,
                    metadataJson = JSONObject().apply {
                        put("projectName", "Social Media Growth")
                        put("currentState", "Planning")
                        put("whatNeedsToBeDone", "Finalize 2026 calendar")
                        put("milestones", "10k Followers")
                        put("nextActions", "Review analytics")
                    }.toString()
                )
            )

            // Pastel Blue - Content Strategy
            noteDao.insertNote(
                NoteEntity(
                    title = "Content Strategy",
                    content = "Would need time to get insights (goals, personas, budget, audits), but after, it would be good to focus on assembling my team (start with SEO specialist, then perhaps an email marketer?). Also need to brainstorm on tooling.",
                    category = "Projects",
                    colorHex = "#E1F5FE", // Pastel Blue
                    tags = "Strategy, Team",
                    isPinned = false,
                    metadataJson = JSONObject().apply {
                        put("projectName", "Content Engine")
                        put("currentState", "Discovery")
                        put("whatNeedsToBeDone", "Assemble team & review tools")
                    }.toString()
                )
            )

            // Pastel Pink - Email A/B Tests
            noteDao.insertNote(
                NoteEntity(
                    title = "Email A/B Tests",
                    content = "- Subject lines\n- Sender name\n- Primary CTA button\n- Sending times and frequency",
                    category = "Tech",
                    colorHex = "#FCE4EC", // Pastel Pink
                    tags = "Email, Growth, Tech",
                    isPinned = false,
                    metadataJson = JSONObject().apply {
                        put("title", "Email A/B Optimization")
                        put("sourceName", "Newsletter Lab")
                        put("specialNotes", "Test on 20% sample first")
                    }.toString()
                )
            )

            // Pastel Orange - Banner Ads
            noteDao.insertNote(
                NoteEntity(
                    title = "Banner Ads",
                    content = "Notes from the workshop:\n- Sizing matters\n- Choose distinctive imagery\n- The landing page must match the display ad",
                    category = "Other",
                    colorHex = "#FFE0B2", // Pastel Orange
                    tags = "Design, Workshop",
                    isPinned = false,
                    metadataJson = JSONObject().apply {
                        put("summary", "Creative guidelines for display ad campaigns")
                    }.toString()
                )
            )

            // Additional sample note for Travel
            noteDao.insertNote(
                NoteEntity(
                    title = "Kyoto & Tokyo Exploration",
                    content = "Exploration itinerary across historical shrines and modern tech hubs.",
                    category = "Travel",
                    colorHex = "#E8F5E9", // Pastel Mint
                    tags = "Travel, Japan",
                    isPinned = false,
                    metadataJson = JSONObject().apply {
                        put("destination", "Kyoto & Tokyo")
                        put("location", "Japan")
                        put("budget", "$3,500")
                        put("travelDates", "Autumn 2026")
                        put("details", "JR Pass, Shinkansen booking, Gion district walking tour")
                    }.toString()
                )
            )

            // 3. Initial Tasks
            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L
            taskDao.insertTask(
                TaskEntity(
                    title = "Review Q4 marketing roadmap",
                    description = "Align deliverables with social and email teams",
                    dueDate = now,
                    dueTime = "14:00",
                    priority = "HIGH",
                    status = "PENDING",
                    category = "Projects",
                    tags = "Marketing"
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Send monthly investor update",
                    description = "Include metrics from recent product release",
                    dueDate = now,
                    dueTime = "17:30",
                    priority = "MEDIUM",
                    status = "PENDING",
                    category = "Projects",
                    tags = "Operations"
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Book Shinkansen tickets",
                    description = "Tokyo to Kyoto reserved seats",
                    dueDate = now + (2 * oneDay),
                    dueTime = "10:00",
                    priority = "LOW",
                    status = "PENDING",
                    category = "Travel",
                    tags = "Japan"
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Refactor database repositories",
                    description = "Ensure reactive Flow streaming for notes",
                    dueDate = now + (5 * oneDay),
                    dueTime = "16:00",
                    priority = "MEDIUM",
                    status = "PENDING",
                    category = "Tech",
                    tags = "Coding"
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Complete cardiology mechanism study notes",
                    description = "Review ACE inhibitors vs ARBs pharmacology",
                    dueDate = now + (7 * oneDay),
                    dueTime = "19:00",
                    priority = "HIGH",
                    status = "PENDING",
                    category = "Clinical",
                    tags = "Cardiology"
                )
            )

            // 4. Initial Transactions
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Consulting Retainer",
                    amount = 3200.0,
                    type = "INCOME",
                    category = "Work",
                    date = now - (3 * oneDay),
                    notes = "Monthly client retainer"
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Cloud Server Hosting",
                    amount = 85.50,
                    type = "EXPENSE",
                    category = "Tech",
                    date = now - (2 * oneDay),
                    notes = "Google Cloud Platform"
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Team Workshop Catering",
                    amount = 142.00,
                    type = "EXPENSE",
                    category = "Office",
                    date = now - oneDay,
                    notes = "Lunch for design sprint"
                )
            )

            // 5. Initial Chat Session
            val sessionId = chatDao.insertSession(
                ChatSessionEntity(
                    title = "Workspace AI Assistant",
                    model = "gemini-3.5-flash",
                    createdAt = now,
                    updatedAt = now
                )
            )
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "model",
                    content = "Hello! I am your AI workspace assistant. I can help brainstorm ideas, structure notes (Travel, Projects, Tech, Clinical), summarize long discussions, or draft task checklists. How can I assist you today?",
                    timestamp = now
                )
            )
        }
    }
}
