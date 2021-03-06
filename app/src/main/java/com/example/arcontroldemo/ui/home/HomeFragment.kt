package com.example.arcontroldemo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arcontroldemo.R
import com.example.arcontroldemo.databinding.DemoItemBinding
import com.example.arcontroldemo.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.demoList.apply {
            adapter = DemoListRecycleViewAdapter(
                arrayOf(
                    Triple(
                        getString(R.string.title_touch_place_model),
                        getString(R.string.string_desc_touch_screen_place_model),
                        R.id.action_navigation_home_to_touchScreenPlaceModelFragment
                    ),
                    Triple(
                        getString(R.string.title_model_indicator),
                        getString(R.string.string_desc_model_indicator),
                                R.id.action_navigation_home_to_modelIndicatorFragment
                    ),
                    Triple(
                        getString(R.string.title_gesture_control),
                        getString(R.string.string_desc_gesture_control),
                        R.id.action_navigation_home_to_fingerControlModelFragment
                    ),
                    Triple(
                        getString(R.string.title_model_animation),
                        getString(R.string.string_desc_model_animation),
                        R.id.action_navigation_home_to_animateModelFragment
                    ),
                )
            )
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    LinearLayoutManager.VERTICAL
                )
            )
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DemoListRecycleViewAdapter(private val dataSet: Array<Triple<String, String, Int>>) :
        RecyclerView.Adapter<DemoListRecycleViewAdapter.ViewHolder>() {

        /**
         * Provide a reference to the type of views that you are using
         * (custom ViewHolder).
         */
        inner class ViewHolder(private val binding: DemoItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(position: Int) {
                binding.title.text = dataSet[position].first
                binding.desc.text = dataSet[position].second
                binding.root.setOnClickListener {
                    findNavController().navigate(dataSet[position].third)
                }
            }
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(viewGroup.context),
                    viewType,
                    viewGroup,
                    false
                )
            )

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            viewHolder.bind(position)
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = dataSet.size

        override fun getItemViewType(position: Int): Int = R.layout.demo_item

    }
}