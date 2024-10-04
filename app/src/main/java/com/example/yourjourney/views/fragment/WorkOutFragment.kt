package com.example.yourjourney.views.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.yourjourney.R
import com.example.yourjourney.adapters.ExerciseGifAdapter
import com.example.yourjourney.adapters.WorkoutAdapter
import com.example.yourjourney.data.plan.ExerciseLog
import com.example.yourjourney.data.plan.ExercisePlan
import com.example.yourjourney.data.plan.Plan
import com.example.yourjourney.data.results.WorkoutResult
import com.example.yourjourney.posedetector.PoseDetectorProcessor
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.CHEST_PRESS_CLASS
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.DEAD_LIFT_CLASS
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.LUNGES_CLASS
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.POSE_CLASSES
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.PUSHUPS_CLASS
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.SHOULDER_PRESS_CLASS
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.SITUP_UP_CLASS
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.SQUATS_CLASS
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.WARRIOR_CLASS
import com.example.yourjourney.posedetector.classification.PoseClassifierProcessor.YOGA_TREE_CLASS
import com.example.yourjourney.util.MemoryManagement
import com.example.yourjourney.util.MyApplication
import com.example.yourjourney.util.MyUtils.Companion.convertTimeStringToMinutes
import com.example.yourjourney.util.MyUtils.Companion.databaseNameToClassification
import com.example.yourjourney.util.MyUtils.Companion.exerciseNameToDisplay
import com.example.yourjourney.util.VisionImageProcessor
import com.example.yourjourney.viewmodels.AddPlanViewModel
import com.example.yourjourney.viewmodels.CameraXViewModel
import com.example.yourjourney.viewmodels.HomeViewModel
import com.example.yourjourney.viewmodels.ResultViewModel
import com.example.yourjourney.views.activity.MainActivity
import com.example.yourjourney.views.fragment.preference.PreferenceUtils
import com.example.yourjourney.views.graphic.GraphicOverlay
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.common.MlKitException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask


class WorkOutFragment : Fragment(), MemoryManagement {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var screenOn = false
    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var selectedModel = POSE_DETECTION
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraSelector: CameraSelector? = null
    private var today: String = DateFormat.format("EEEE", Date()) as String
    private var runOnce: Boolean = false
    private var isAllWorkoutFinished: Boolean = false
    private var mRecTimer: Timer? = null
    private var mRecSeconds = 0
    private var mRecMinute = 0
    private var mRecHours = 0
    private val onlyExercise: List<String> =
        listOf(
            SQUATS_CLASS,
            PUSHUPS_CLASS,
            LUNGES_CLASS,
            SITUP_UP_CLASS,
            CHEST_PRESS_CLASS,
            DEAD_LIFT_CLASS,
            SHOULDER_PRESS_CLASS
        )
    private val onlyPose: List<String> = listOf(WARRIOR_CLASS, YOGA_TREE_CLASS)
    private var notCompletedExercise: List<Plan>? = null

    // late init properties---
    private lateinit var resultViewModel: ResultViewModel
    private lateinit var timerTextView: TextView
    private lateinit var timerRecordIcon: ImageView
    private lateinit var workoutRecyclerView: RecyclerView
    private lateinit var workoutAdapter: WorkoutAdapter
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var addPlanViewModel: AddPlanViewModel
    private lateinit var startButton: Button

    private lateinit var buttonCancelExercise: Button
    private lateinit var cameraFlipFAB: FloatingActionButton
    private lateinit var confIndicatorView: ImageView
    private lateinit var currentExerciseTextView: TextView
    private lateinit var currentRepetitionTextView: TextView
    private lateinit var confidenceTextView: TextView
    private lateinit var cameraViewModel: CameraXViewModel
    private lateinit var loadingTV: TextView
    private lateinit var loadProgress: ProgressBar
    private lateinit var completeAllExercise: TextView
    private lateinit var skipButton: Button
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var yogaPoseImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }
        initTextToSpeech()
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        cameraViewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory
                .getInstance(requireActivity().application)
        )[CameraXViewModel::class.java]
        resultViewModel = ResultViewModel(MyApplication.getInstance())
        addPlanViewModel = AddPlanViewModel(MyApplication.getInstance())
        homeViewModel = HomeViewModel(MyApplication.getInstance())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_workout, container, false)
        // Linking all button and controls
        cameraFlipFAB = view.findViewById(R.id.facing_switch)
        startButton = view.findViewById(R.id.button_start_exercise)

        buttonCancelExercise = view.findViewById(R.id.button_cancel_exercise)
        timerTextView = view.findViewById(R.id.timerTV)
        timerRecordIcon = view.findViewById(R.id.timerRecIcon)
        confIndicatorView = view.findViewById(R.id.confidenceIndicatorView)
        currentExerciseTextView = view.findViewById(R.id.currentExerciseText)
        currentRepetitionTextView = view.findViewById(R.id.currentRepetitionText)
        confidenceTextView = view.findViewById(R.id.confidenceIndicatorTextView)
        completeAllExercise = view.findViewById(R.id.completedAllExerciseTextView)
        confIndicatorView.visibility = View.INVISIBLE
        confidenceTextView.visibility = View.INVISIBLE
        loadingTV = view.findViewById(R.id.loadingStatus)
        loadProgress = view.findViewById(R.id.loadingProgress)
        skipButton = view.findViewById(R.id.skipButton)
        workoutRecyclerView = view.findViewById(R.id.workoutRecycleViewArea)
        workoutRecyclerView.layoutManager = LinearLayoutManager(activity)
        yogaPoseImage = view.findViewById(R.id.yogaPoseSnapShot)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Initialize views
        super.onViewCreated(view, savedInstanceState)
        previewView = view.findViewById(R.id.preview_view)
        val gifContainer: FrameLayout = view.findViewById(R.id.gifContainer)
        graphicOverlay = view.findViewById(R.id.graphic_overlay)
        cameraFlipFAB.visibility = View.VISIBLE
        startButton.visibility = View.VISIBLE
        gifContainer.visibility = View.GONE
        skipButton.visibility = View.GONE

        startButton.setOnClickListener {
            synthesizeSpeech("Kindly Follow the Poses illustrated")
            // showing loading AI pose detection Model information to user
            loadingTV.visibility = View.GONE
            loadProgress.visibility = View.GONE
            // Set the screenOn flag to true, preventing the screen from turning off
            screenOn = true
            // Add the FLAG_KEEP_SCREEN_ON flag to the activity's window, keeping the screen on
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            cameraFlipFAB.visibility = View.GONE
            gifContainer.visibility = View.VISIBLE
            buttonCancelExercise.visibility = View.VISIBLE

            startButton.visibility = View.GONE
            // To disable screen timeout
            //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            cameraViewModel.triggerClassification.value = true
        }

        // Cancel the exercise
        buttonCancelExercise.setOnClickListener {
            synthesizeSpeech("Workout Cancelled")
            stopMediaTimer()
            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigationView?.selectedItemId = R.id.navigation_exercise
            screenOn = false
            // Clear the FLAG_KEEP_SCREEN_ON flag to allow the screen to turn off
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // stop triggering classification process
            cameraViewModel.triggerClassification.value = false
        }

        // 10 reps =  3.2 for push up -> 1 reps = 3.2/10
        // Complete the exercise
        val sitUp = Postures.situp
        val pushUp = Postures.pushup
        val lunge = Postures.lunge
        val squat = Postures.squat
        val chestPress = Postures.chestpress
        val deadLift = Postures.deadlift
        val shoulderPress = Postures.shoulderpress



        cameraViewModel.processCameraProvider.observe(viewLifecycleOwner) { provider: ProcessCameraProvider? ->
            cameraProvider = provider
            //bindAllCameraUseCases()
            notCompletedExercise?.let { bindAllCameraUseCases(it) } ?: bindAllCameraUseCases(
                emptyList()
            )
        }

        cameraFlipFAB.setOnClickListener {
            toggleCameraLens()
        }
        // initialize the list of plan exercise to be filled from database
        val databaseExercisePlan = mutableListOf<ExercisePlan>()
        // Initialize Exercise Log
        val exerciseLog = ExerciseLog()
        // get the list of plans from database
        lifecycleScope.launch(Dispatchers.IO) {

            // get not completed exercise from database using coroutine
            notCompletedExercise =
                withContext(Dispatchers.IO) { homeViewModel.getNotCompletePlans(today) }

            // Create a set to track unique exercises
            val uniqueExercises = mutableSetOf<String>()

            // Create exerciseGifs list based on notCompletedExercise, avoiding duplicates
            val exerciseGifs = mutableListOf<Pair<String, Int>>()

            // Function to add exercise to the list if it's not already present
            fun addExerciseIfNotPresent(exercise: String) {
                if (uniqueExercises.add(exercise)) {
                    exerciseGifs.add(exercise to mapExerciseToDrawable(exercise))
                }
            }

            // Add entries based on notCompletedExercise
            notCompletedExercise?.let {
                it.map { plan ->
                    addExerciseIfNotPresent(plan.exercise)
                }
            }

            // Add entries for default exercises (squat, lunge, warrior, tree)
            addExerciseIfNotPresent(exerciseNameToDisplay(SQUATS_CLASS))
            addExerciseIfNotPresent(exerciseNameToDisplay(LUNGES_CLASS))
            addExerciseIfNotPresent(exerciseNameToDisplay(WARRIOR_CLASS))
            addExerciseIfNotPresent(exerciseNameToDisplay(YOGA_TREE_CLASS))

            val viewPager: ViewPager2 = view.findViewById(R.id.exerciseViewPager)
            val exerciseGifAdapter = ExerciseGifAdapter(exerciseGifs) {
                // Handle skip button click here
                // Transition to the "Start" button
                startButton.visibility = View.GONE
                cameraFlipFAB.visibility = View.VISIBLE
                viewPager.visibility = View.GONE
                skipButton.visibility = View.GONE
                gifContainer.visibility = View.GONE
                cameraFlipFAB.visibility = View.GONE
            }
            viewPager.adapter = exerciseGifAdapter

            notCompletedExercise?.forEach { item ->
                val exercisePlan =
                    ExercisePlan(
                        item.id,
                        databaseNameToClassification(item.exercise),
                        item.repeatCount
                    )
                val existingExercisePlan =
                    databaseExercisePlan.find {
                        it.planId == item.id
                    }
                if (existingExercisePlan != null) {
                    // Update repetitions if ExercisePlan with the same exerciseName already exists
                    existingExercisePlan.repetitions += item.repeatCount
                } else {
                    // Add a new ExercisePlan if not already in the databaseExercisePlan
                    databaseExercisePlan.add(exercisePlan)
                }
            }
            // Push the planned exercise name in exercise Log
            databaseExercisePlan.forEach {
                exerciseLog.addExercise(
                    it.planId,
                    it.exerciseName,
                    0,
                    0f,
                    false
                )
            }
        }

        // Declare variables to store previous values
        var previousKey: String? = null
        var previousConfidence: Float? = null


        cameraViewModel.postureLiveData.observe(viewLifecycleOwner) { mapResult ->
            for ((key, value) in mapResult) {
                // Visualize the repetition exercise data
                if (key in POSE_CLASSES.toList()) {
                    // get the data from exercise log of specific exercise
                    val data = exerciseLog.getExerciseData(key)
                    if (key in onlyExercise && data == null) {
                        // Adding exercise for the first time
                        exerciseLog.addExercise(null, key, value.repetition, value.confidence, false)
                    } else if (key in onlyExercise && value.repetition == data?.repetitions?.plus(1)) {
                        // Check if the exercise is squats and update the repetition text view
                        if (key == Postures.squat.type && value.repetition == 5) {
                            synthesizeSpeech("Congratulation! Squat completed.")
                            markExerciseAsDone("Squats")
                        } else {
                            currentRepetitionTextView.text = "${value.repetition} Repetitions"
                        }
                        workoutRecyclerView.visibility = View.VISIBLE
                        if (isAllWorkoutFinished) {
                            completeAllExercise.visibility = View.VISIBLE
                        } else {
                            completeAllExercise.visibility = View.GONE
                        }
                        confIndicatorView.visibility = View.INVISIBLE
                        confidenceTextView.visibility = View.INVISIBLE
                        yogaPoseImage.visibility = View.INVISIBLE
                        // Check if the exercise target is complete
                        var repetition: Int? = databaseExercisePlan.find {
                            it.exerciseName.equals(key, ignoreCase = true)
                        }?.repetitions
                        if (repetition == null || repetition == 0) {
                            repetition = HighCount
                        }
                        if (!data.isComplete && (value.repetition >= repetition)) {
                            // Adding data only when the increment happens
                            exerciseLog.addExercise(data.planId, key, value.repetition, value.confidence, true)
                            // Inform the user about completion only once
                            synthesizeSpeech(exerciseNameToDisplay(key) + " exercise Complete")
                            // Check if all the exercise list complete
                            if (exerciseLog.areAllExercisesCompleted(databaseExercisePlan)) {
                                val handler = Handler(Looper.getMainLooper())
                                handler.postDelayed({
                                    synthesizeSpeech("Congratulation! all the planned exercise completed")
                                    isAllWorkoutFinished = true
                                    completeAllExercise.visibility = View.VISIBLE
                                }, 5000)
                            }
                            // Update complete status for existing plan
                            if (data.planId != null) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    addPlanViewModel.updateComplete(true, System.currentTimeMillis(), data.planId)
                                }
                            }
                        } else if (data.isComplete) {
                            // Adding data only when the increment happens
                            exerciseLog.addExercise(data.planId, key, value.repetition, value.confidence, true)
                        } else {
                            // Adding data only when the increment happens
                            exerciseLog.addExercise(data.planId, key, value.repetition, value.confidence, false)
                        }
                        // Display Current result when the increment happens
                        displayResult(key, exerciseLog)

                        // Update the display list of all exercise progress when the increment happens
                        val exerciseList = exerciseLog.getExerciseDataList()
                        workoutAdapter = WorkoutAdapter(exerciseList, databaseExercisePlan)
                        workoutRecyclerView.adapter = workoutAdapter
                    } else if (key in onlyPose && value.confidence > 0.5) {
                        if (key != previousKey || value.confidence != previousConfidence) {
                            // Implementation of pose confidence
                            displayConfidence(key, value.confidence)
                            workoutRecyclerView.visibility = View.GONE
                            completeAllExercise.visibility = View.GONE
                            currentExerciseTextView.visibility = View.VISIBLE
                            currentRepetitionTextView.visibility = View.GONE
                            confidenceTextView.visibility = View.VISIBLE
                        }
                    }
                    previousKey = key
                    previousConfidence = value.confidence
                }
            }
        }
    }

    private fun markExerciseAsDone(exerciseName: String) {
        val currentUser = auth.currentUser
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(calendar.time)
        currentUser?.let {
            val userId = it.uid
            val userRef = database.getReference("users").child(userId)
            userRef.child("disease").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(diseaseSnapshot: DataSnapshot) {
                    val disease = diseaseSnapshot.getValue(String::class.java) ?: return

                    userRef.child("stage").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(stageSnapshot: DataSnapshot) {
                            val stage = stageSnapshot.getValue(String::class.java) ?: return
                            val dayString = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
                            // Reference to the exercise's score in the diseases collection
                            val scoreRef = database.getReference("diseases")
                                .child(disease)
                                .child(stage)
                                .child("Exercise")
                                .child(dayString)
                                .child(exerciseName)
                                .child("score")

                            // Fetch the score for the exercise
                            scoreRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(scoreSnapshot: DataSnapshot) {
                                    if (scoreSnapshot.exists()) {
                                        val exerciseScore = scoreSnapshot.getValue(Int::class.java) ?: 0 // Default score if not found

                                        // Now mark the exercise as done
                                        val doneRef = database.getReference("users")
                                            .child(userId)
                                            .child("exercises")
                                            .child(currentDate)
                                            .child(exerciseName)

                                        // Check if already marked as done
                                        doneRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                if (!snapshot.exists()) {
                                                    // Mark as done and set the score for the specific exercise
                                                    val exerciseData = mapOf(
                                                        "done" to true,
                                                        "score" to exerciseScore // Add score only for the specific exercise
                                                    )

                                                    // Update the exercise as done
                                                    doneRef.setValue(exerciseData).addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            // Now update the total score for the date
                                                            updateTotalScore(userId, currentDate, exerciseScore)
                                                        } else {
                                                            Toast.makeText(requireContext(), "Failed to mark $exerciseName as done.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(requireContext(), "$exerciseName is already marked as done for today.", Toast.LENGTH_SHORT).show()
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                    } else {
                                        Toast.makeText(requireContext(), "Score not found for $exerciseName.", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(requireContext(), "Error fetching exercise score: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), "Error fetching stage: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching disease: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun updateTotalScore(userId: String, currentDate: String, exerciseScore: Int) {
        val totalScoreRef = database.getReference("users")
            .child(userId)
            .child("exercises")
            .child(currentDate)
            .child("totalScore")

        // Fetch the existing total score for the date
        totalScoreRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(totalScoreSnapshot: DataSnapshot) {
                val currentTotalScore = totalScoreSnapshot.getValue(Int::class.java) ?: 0
                val newTotalScore = currentTotalScore + exerciseScore

                // Update the total score for the specific date
                totalScoreRef.setValue(newTotalScore).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Total score updated to $newTotalScore for $currentDate.", Toast.LENGTH_SHORT).show()
                        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                        bottomNavigationView?.selectedItemId = R.id.navigation_exercise

                    } else {
                        Toast.makeText(requireContext(), "Failed to update total score for $currentDate.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching total score: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    // Map the notCompletedExercise list to a list of pairs to show gifs
    private fun mapExerciseToDrawable(exercise: String): Int {
        return when (exercise) {
            exerciseNameToDisplay(PUSHUPS_CLASS) -> R.drawable.pushup
            exerciseNameToDisplay(LUNGES_CLASS) -> R.drawable.lunge
            exerciseNameToDisplay(SQUATS_CLASS) -> R.drawable.squats
            exerciseNameToDisplay(SITUP_UP_CLASS) -> R.drawable.situp
            exerciseNameToDisplay(CHEST_PRESS_CLASS) -> R.drawable.chest_press_gif
            exerciseNameToDisplay(DEAD_LIFT_CLASS) -> R.drawable.dead_lift_gif
            exerciseNameToDisplay(SHOULDER_PRESS_CLASS) -> R.drawable.shoulder_press_gif
            exerciseNameToDisplay(WARRIOR_CLASS) -> R.drawable.warrior_yoga_gif
            exerciseNameToDisplay(YOGA_TREE_CLASS) -> R.drawable.tree_yoga_gif
            else -> R.drawable.warrior_yoga_gif
        }
    }

    /**
     * List of yoga images
     */
    private val yogaPoseImages = mapOf(
        WARRIOR_CLASS to R.drawable.warrior_yoga_pose,
        YOGA_TREE_CLASS to R.drawable.tree_yoga_pose
    )

    private fun getDrawableResourceIdYoga(yogaPoseKey: String): Int {
        return yogaPoseImages[yogaPoseKey]
            ?: throw IllegalArgumentException("Invalid yoga pose key: $yogaPoseKey")
    }

    /**
     * Initialize TextToSpeech engine
     */
    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                // Set language to US English and speech rate to 1.0
                textToSpeech.language = Locale.US
                textToSpeech.setSpeechRate(1.0f)
            }
        }
    }

    /**
     * Synthesize speech using TextToSpeech
     */
    private fun synthesizeSpeech(name: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            textToSpeech.speak(name, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    /**
     * Display exercise result in the UI
     */
    @SuppressLint("SetTextI18n")
    private fun displayResult(key: String, exerciseLog: ExerciseLog) {
        currentExerciseTextView.visibility = View.VISIBLE
        currentRepetitionTextView.visibility = View.VISIBLE
        val data = exerciseLog.getExerciseData(key)
        currentExerciseTextView.text = exerciseNameToDisplay(key)
        currentRepetitionTextView.text = "count: " + data?.repetitions.toString()
    }

    /**
     * Display confidence level with different colors based on thresholds
     */
    private fun displayConfidence(key: String, confidence: Float) {
        confIndicatorView.visibility = View.VISIBLE
        yogaPoseImage.visibility = View.VISIBLE
        if (confidence <= 0.6f) {
            confIndicatorView.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.lightGreen)
        } else if (confidence > 0.6f && confidence <= 0.7f) {
            confIndicatorView.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.orange)
        } else if (confidence > 0.7f && confidence <= 0.8f) {
            confIndicatorView.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.yellow)
        } else if (confidence > 0.8f && confidence <= 0.9f) {
            confIndicatorView.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.lightGreen)
        } else {
            confIndicatorView.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.green)
        }
    }

    /**
     * Bind all camera use cases (preview and analysis)
     */
    private fun bindAllCameraUseCases(notCompletedPlan: List<Plan>) {
        // Bind all camera use cases (preview and analysis)
        bindPreviewUseCase()
        cameraViewModel.triggerClassification.observe(viewLifecycleOwner) { pressed ->
            bindAnalysisUseCase(pressed, notCompletedPlan)
        }
    }

    /**
     * bind preview use case
     */
    @Suppress("DEPRECATION")
    private fun bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(requireContext())) {
            return
        }
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }
        val builder = Preview.Builder()
        val targetResolution =
            PreferenceUtils.getCameraXTargetResolution(requireContext(), lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(previewView!!.surfaceProvider)
        camera = cameraProvider!!.bindToLifecycle(this, cameraSelector!!, previewUseCase)
    }

    /**
     * bind analysis use case
     */
    private fun bindAnalysisUseCase(runClassification: Boolean, notCompletedPlan: List<Plan>) {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider?.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor?.stop()
        }
        imageProcessor = try {
            when (selectedModel) {
                POSE_DETECTION -> {
                    // get all the setting preferences for camera x live preview
                    val poseDetectorOptions =
                        PreferenceUtils.getPoseDetectorOptionsForLivePreview(requireContext())
                    val shouldShowInFrameLikelihood =
                        PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(
                            requireContext()
                        )
                    val visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(requireContext())
                    val rescaleZ =
                        PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(requireContext())

                    PoseDetectorProcessor(
                        requireContext(),
                        poseDetectorOptions,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        runClassification,
                        true,
                        cameraViewModel,
                        notCompletedPlan
                    )
                }

                else -> throw IllegalStateException("Invalid model name")
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Can not create image processor: $selectedModel",
                e
            )
            Toast.makeText(
                requireContext(),
                "Can not create image processor: " + e.localizedMessage,
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val builder = ImageAnalysis.Builder()
        analysisUseCase = builder.build()
        needUpdateGraphicOverlayImageSourceInfo = true
        analysisUseCase?.setAnalyzer(
            ContextCompat.getMainExecutor(requireContext())
        ) { imageProxy: ImageProxy ->
            if (needUpdateGraphicOverlayImageSourceInfo) {
                val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    graphicOverlay!!.setImageSourceInfo(
                        imageProxy.width,
                        imageProxy.height,
                        isImageFlipped
                    )
                } else {
                    graphicOverlay!!.setImageSourceInfo(
                        imageProxy.height,
                        imageProxy.width,
                        isImageFlipped
                    )
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            }

            try {
                imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
            } catch (e: MlKitException) {
                Log.e(
                    TAG,
                    "Failed to process image. Error: " + e.localizedMessage
                )
                Toast.makeText(
                    requireContext(),
                    e.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        cameraProvider?.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
    }

    /**
     * Check if all required runtime permissions are granted
     */
    private fun allRuntimePermissionsGranted(): Boolean {
        // Check if all required runtime permissions are granted
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(requireContext(), it)) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Check if a specific permission is granted
     */
    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        // Check if a specific permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

    /**
     * Request runtime permissions
     */
    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(requireContext(), it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    /**
    Toggle between front and back camera lenses
     *
     */
    private fun toggleCameraLens() {
        if (cameraProvider == null) {
            Log.d(TAG, "Camera provider is null")
            return
        }
        val newLensFacing =
            if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
        val newCameraSelector = CameraSelector.Builder().requireLensFacing(newLensFacing).build()

        try {
            if (cameraProvider!!.hasCamera(newCameraSelector)) {
                Log.d(TAG, "Set facing to $newLensFacing")
                lensFacing = newLensFacing
                cameraSelector = newCameraSelector
                notCompletedExercise?.let { bindAllCameraUseCases(it) } ?: bindAllCameraUseCases(
                    emptyList()
                )
                return
            }
        } catch (e: CameraInfoUnavailableException) {
            Log.e(TAG, "Failed to get camera info", e)
        }
    }

    /**
     * timer handling coroutine
     */
    private val mMainHandler: Handler by lazy {
        Handler(Looper.getMainLooper()) {
            when (it.what) {
                WHAT_START_TIMER -> {
                    if (mRecSeconds % 2 != 0) {
                        timerRecordIcon.visibility = View.VISIBLE
                    } else {
                        timerRecordIcon.visibility = View.INVISIBLE
                    }
                    timerTextView.text = calculateTime(mRecSeconds, mRecMinute)
                }

                WHAT_STOP_TIMER -> {
                    timerTextView.text = calculateTime(0, 0)
                    timerRecordIcon.visibility = View.GONE
                    timerTextView.visibility = View.GONE
                }
            }
            true
        }
    }


    /**
     * Start timer functionality
     */
    private fun startMediaTimer() {
        val pushTask: TimerTask = object : TimerTask() {
            override fun run() {
                mRecSeconds++
                if (mRecSeconds >= 60) {
                    mRecSeconds = 0
                    mRecMinute++
                }
                if (mRecMinute >= 60) {
                    mRecMinute = 0
                    mRecHours++
                    if (mRecHours >= 24) {
                        mRecHours = 0
                        mRecMinute = 0
                        mRecSeconds = 0
                    }
                }
                mMainHandler.sendEmptyMessage(WHAT_START_TIMER)
            }
        }
        if (mRecTimer != null) {
            stopMediaTimer()
        }
        mRecTimer = Timer()
        mRecTimer?.schedule(pushTask, 1000, 1000)
    }


    /**
     * Stop timer functionality
     */
    private fun stopMediaTimer() {
        if (mRecTimer != null) {
            mRecTimer?.cancel()
            mRecTimer = null
        }
        mRecHours = 0
        mRecMinute = 0
        mRecSeconds = 0
        mMainHandler.sendEmptyMessage(WHAT_STOP_TIMER)
    }

    /**
     * Calculate the time and return string
     */
    private fun calculateTime(seconds: Int, minute: Int, hour: Int? = null): String {
        val mBuilder = java.lang.StringBuilder()

        if (hour != null) {
            if (hour < 10) {
                mBuilder.append("0")
                mBuilder.append(hour)
            } else {
                mBuilder.append(hour)
            }
            mBuilder.append(":")
        }

        if (minute < 10) {
            mBuilder.append("0")
            mBuilder.append(minute)
        } else {
            mBuilder.append(minute)
        }

        mBuilder.append(":")
        if (seconds < 10) {
            mBuilder.append("0")
            mBuilder.append(seconds)
        } else {
            mBuilder.append(seconds)
        }
        return mBuilder.toString()
    }


    /**
     * overridden function to clean up memory, clear object reference and un-register onClickListener
     * in WorkOutFragment
     */
    override fun clearMemory() {
        if (!textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        previewView = null
        graphicOverlay = null
        cameraProvider = null
        camera = null
        previewUseCase = null
        analysisUseCase = null
        imageProcessor = null
        cameraSelector = null
        mRecTimer?.let {
            it.cancel()
            mRecTimer = null
        }
        startButton.setOnClickListener(null)

        buttonCancelExercise.setOnClickListener(null)
        cameraFlipFAB.setOnClickListener(null)
        skipButton.setOnClickListener(null)
        workoutRecyclerView.adapter = null
    }

    override fun onDestroy() {
        clearMemory()
        super.onDestroy()
    }

    /**
     *Constants and companion object
     */
    companion object {
        private const val TAG = "RepDetect CameraXLivePreview"
        private const val POSE_DETECTION = "Pose Detection"
        private const val PERMISSION_REQUESTS = 1

        private const val WHAT_START_TIMER = 0x00
        private const val WHAT_STOP_TIMER = 0x01
        private const val HighCount = 9999999

        private val REQUIRED_RUNTIME_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
    }

    /**
     * Typed constant class for exercise postures
     */
    class TypedConstant(val type: String, val value: Double)
    object Postures {
        val pushup = TypedConstant(PUSHUPS_CLASS, 3.2)
        val lunge = TypedConstant(LUNGES_CLASS, 3.0)
        val squat = TypedConstant(SQUATS_CLASS, 3.8)
        val situp = TypedConstant(SITUP_UP_CLASS, 5.0)
        val chestpress = TypedConstant(CHEST_PRESS_CLASS, 7.0)
        val deadlift = TypedConstant(DEAD_LIFT_CLASS, 10.0)
        val shoulderpress = TypedConstant(SHOULDER_PRESS_CLASS, 9.0)
    }
}