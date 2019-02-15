package com.rndapp.task_feed.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.android.volley.Response
import com.rndapp.task_feed.R
import com.rndapp.task_feed.adapters.*
import com.rndapp.task_feed.api.ProjectRequest
import com.rndapp.task_feed.api.ProjectsRequest
import com.rndapp.task_feed.api.SprintProjectRequest
import com.rndapp.task_feed.api.VolleyManager
import com.rndapp.task_feed.listeners.OnProjectClickedListener
import com.rndapp.task_feed.models.*
import kotlinx.android.synthetic.main.standard_recycler.*

/**
 * Created by ell on 12/6/17.
 */
class ChooserActivity: AppCompatActivity() {
    companion object {
        val SPRINT = "SPRINT"
        val PROJECT = "PROJECT"
        val SPRINT_PROJECT = "SPRINT_PROJECT"
        val SPRINT_PROJECT_ID = "SPRINT_PROJECT_ID"
        val SPRINT_PROJECTS = "SPRINT_PROJECTS"
        val PROJECTS = "PROJECTS"
        val TASKS = "TASKS" //provide project
        val DAY = "DAY" // provide sprint
        val ARRAY = "ARRAY"

        val CHOOSER_TYPE = "CHOOSER_TYPE"

        val PROJECT_ID = "PROJECT_ID"
        val SPRINT_ID = "SPRINT_ID"
    }

    var selectedSprintProjects = ArrayList<SprintProject>()
    var selectedProjects = ArrayList<Project>()
    var selectedTasks = ArrayList<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.standard_recycler)

        fab.setImageResource(R.drawable.baseline_check_white_24)

        val extras = intent?.extras
        if (extras != null) {
            val array = extras.getSerializable(ARRAY)
            if (array != null && array is ArrayList<*>) {
                setupList(array)
            } else {
                setupType(extras)
            }
        }
    }

    fun setupList(array: ArrayList<*>) {
        if (array.size == 0) {
            setResult(Activity.RESULT_CANCELED)
        } else {
            when (array.first()) {
                is Project -> {
                    (fab as View).visibility = View.GONE
                    setupAdapter(ProjectsAdapter(array as java.util.ArrayList<Project>, object: OnProjectClickedListener {
                        override fun onProjectClicked(project: Project) {
                            val intent = Intent()
                            intent.putExtra(PROJECT, project)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                    }) as RecyclerView.Adapter<RecyclerView.ViewHolder>)
                }
                is SprintProject -> {
                    (fab as View).visibility = View.GONE
                    setupAdapter(SprintProjectsAdapter(array as java.util.ArrayList<SprintProject>) {
                        val intent = Intent()
                        intent.putExtra(SPRINT_PROJECT, it)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    } as RecyclerView.Adapter<RecyclerView.ViewHolder>)
                }
                is Task -> {
                    setupAdapter(taskAdapterWith(array as java.util.ArrayList<Task>))
                }
                is Day -> {

                }
                is Sprint -> {

                }
            }
        }
    }

    fun setupType(extras: Bundle) {
        refreshLayout.isRefreshing = true
        val type = extras.get(CHOOSER_TYPE)
        if (type != null) {
            when(type) {
                PROJECTS -> setupAllProjects()
                SPRINT -> setupAllSprints()
                TASKS -> {
                    val sprintProjectId = extras.getInt(SPRINT_PROJECT_ID, -1)
                    val sprintId = extras.getInt(SPRINT_ID, -1)
                    if (sprintProjectId != -1) {
                        setupSingleSprintProject(sprintId, sprintProjectId)
                    } else {
                        setupSingleProject(extras.getInt(PROJECT_ID, -1))
                    }
                }
                DAY -> {
                    //TODO handle day choosing
                }
            }
        }
    }

    fun setupSingleSprintProject(sprintId: Int, projectId: Int) {
        val request = SprintProjectRequest(projectId, Response.Listener { project ->
            val tasks = project.unfinishedTasks()
            if (tasks != null) {
                setupAdapter(taskAdapterWith(tasks))
            }

            refreshLayout.isRefreshing = false
        }, Response.ErrorListener { error ->
            error.printStackTrace()

            refreshLayout.isRefreshing = false
        })
        VolleyManager.queue?.add(request)
    }

    fun setupSingleProject(projectId: Int) {
        val request = ProjectRequest(projectId, Response.Listener { project ->
            val tasks = project.tasks?.filter { !it.isFinished }
            if (tasks != null) {
                setupAdapter(taskAdapterWith(tasks))
            }

            refreshLayout.isRefreshing = false
        }, Response.ErrorListener { error ->
            error.printStackTrace()
            refreshLayout.isRefreshing = false
        })
        VolleyManager.queue?.add(request)
    }

    fun setupAllProjects() {
        val request = ProjectsRequest(Response.Listener { projects ->
            setupAdapter(projectAdapterWith(projects))

            refreshLayout.isRefreshing = false
        }, Response.ErrorListener { error ->
            error.printStackTrace()

            refreshLayout.isRefreshing = false
        })
        VolleyManager.queue?.add(request)
    }

    fun setupAllSprints() {

    }

    fun setupAdapter(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) {
        val manager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val recyclerView = findViewById(R.id.recyclerView) as RecyclerView
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
    }

    fun projectAdapterWith(projects: ArrayList<Project>): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        fab.setOnClickListener {
            val intent = Intent()
            intent.putExtra(PROJECTS, selectedProjects)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        return HighlightableProjectAdapter(projects) { project, isHighlighted ->
            if (isHighlighted) {
                selectedProjects.add(project)
            } else {
                selectedProjects.remove(project)
            }
        }
    }

    fun taskAdapterWith(tasks: List<Task>): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        fab.setOnClickListener {
            val intent = Intent()
            intent.putExtra(TASKS, selectedTasks)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        return HighlightableTaskAdapter(ArrayList(tasks)) { task, isHighlighted ->
            if (isHighlighted) {
                selectedTasks.add(task)
            } else {
                selectedTasks.remove(task)
            }
        }
    }
}
