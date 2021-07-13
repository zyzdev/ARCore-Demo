package com.example.arcontroldemo.customize

import android.animation.ObjectAnimator
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.example.arcontroldemo.R
import com.google.android.material.button.MaterialButton

open class BaseFragment : Fragment() {
    protected fun showHint(binding: ViewDataBinding, hint: String, canCancel: Boolean = true) {
        binding.root.apply {
            findViewById<TextView>(R.id.hint_info)?.text = hint
            findViewById<TextView>(R.id.ok)?.apply {
                visibility = if (canCancel) View.VISIBLE else View.GONE
                if (canCancel)
                    setOnClickListener {
                        hideHint(binding)
                    }
            }

            findViewById<CardView>(R.id.hint_layout)?.also {
                ObjectAnimator.ofFloat(it, "alpha", it.alpha, 1f).apply {
                    it.visibility = View.VISIBLE
                    start()
                }
            }
        }
    }

    protected fun hideHint(binding: ViewDataBinding) {
        binding.root.apply {
            findViewById<CardView>(R.id.hint_layout)?.also {
                ObjectAnimator.ofFloat(it, "alpha", it.alpha, 0f).apply {
                    doOnEnd { it2 ->
                        it.visibility = View.GONE
                    }
                    start()
                }
            }
        }
    }
}