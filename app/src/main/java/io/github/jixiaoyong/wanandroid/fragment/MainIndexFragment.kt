package io.github.jixiaoyong.wanandroid.fragment

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import cf.android666.applibrary.Logger
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.github.jixiaoyong.wanandroid.R
import io.github.jixiaoyong.wanandroid.adapter.MainIndexPagingAdapter
import io.github.jixiaoyong.wanandroid.adapter.SearchAdapter
import io.github.jixiaoyong.wanandroid.base.BaseFragment
import io.github.jixiaoyong.wanandroid.base.toast
import io.github.jixiaoyong.wanandroid.utils.*
import io.github.jixiaoyong.wanandroid.view.BannerViewHelper
import io.github.jixiaoyong.wanandroid.view.DispatchNestedScrollView
import io.github.jixiaoyong.wanandroid.viewmodel.MainViewModel
import io.github.jixiaoyong.wanandroid.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main_index.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * author: jixiaoyong
 * email: jixiaoyong1995@gmail.com
 * website: https://jixiaoyong.github.io
 * date: 2019-11-06
 * description: 首页
 */
class MainIndexFragment : BaseFragment() {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var searchViewModel: SearchViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main_index, container, false)
        mainViewModel = ViewModelProviders.of(requireActivity(),
                InjectUtils.provideMainViewModelFactory()).get(MainViewModel::class.java)
        searchViewModel = ViewModelProviders.of(requireActivity(),
                InjectUtils.provideSearchModelFactory()).get(SearchViewModel::class.java)

        setupFakeStateBar(view.stateBarView)

        initView(view)
        return view
    }

    private fun initView(view: View) {
        // 在这里初始化recycleView的高度 = 屏幕高度 - bottomNavView
        val rootView = requireActivity().window.decorView.rootView
        rootView.post {
            val listHeight = rootView.height - requireActivity().bottomNavView.height
            val listParams = view.postRecyclerView.layoutParams
            listParams.height = listHeight
            view.postRecyclerView.layoutParams = listParams
        }

        view.nestedScrollView.isNeedInterceptActionMove = this::isNeedIntercept
        view.swipeRefreshLayout.setOnRefreshListener {
            launch {
                withContext(Dispatchers.IO) {
                    mainViewModel.loadIndexPostFromZero()
                }
                view.swipeRefreshLayout.isRefreshing = false
            }
        }

        val postAdapter = MainIndexPagingAdapter(mainViewModel::updateIndexPostCollectState,
                isLogin = mainViewModel::isLogin)
        view.postRecyclerView.adapter = postAdapter
        view.postRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        mainViewModel.allIndexPost.observe(this, Observer(postAdapter::submitList))

        mainViewModel.bannerListLiveData.observe(this, Observer { list ->
            list?.let { dataList ->
                Logger.d("banner data list $dataList")
                val fragments = BannerViewHelper.initImageBannerOf(
                        dataList.size) { imageView, i ->
                    val imgUrl = dataList.getOrNull(i)?.imagePath
                    imageView.setOnClickListener {
                        dataList.getOrNull(i)?.url?.let {
                            NetUtils.loadUrl(requireContext(), it)
                        }
                    }
                    Glide.with(imageView)
                            .load(imgUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_loading)
                            .error(R.drawable.ic_loading)
                            .centerCrop()
                            .into(imageView)
                }

                view.banner.setViewsAndIndicator(childFragmentManager, fragments, dataList.map {
                    it.title
                })
            }
        })

        view.weChatBtn.setOnClickListener {
            goMoreFragment(view, CommonConstants.Action.WECHAT)
        }
        view.favoriteBtn.setOnClickListener {
            if (mainViewModel.isLogin.value == true) {
                goMoreFragment(view, CommonConstants.Action.FAVORITE)
            } else {
                toast(getString(R.string.tips_plz_login))
            }
        }
        view.peopleBtn.setOnClickListener {
            goMoreFragment(view, CommonConstants.Action.PEOPLE)
        }

        view.searchView.setOnQueryTextFocusChangeListener { v, hasFocus ->
            refreshSearchView()
            if (hasFocus) {
                launch {
                    view.progressBar.visibility = View.VISIBLE
                    val hotKeyWords = withContext(Dispatchers.IO) {
                        searchViewModel.getHotSearchWords()
                    }
                    view.searchList.adapter = SearchAdapter(hotKeyWords, this@MainIndexFragment::searchKeyWord)
                    view.progressBar.visibility = View.GONE
                }
            }
        }

        view.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Logger.d("onQueryTextSubmit:$query")
                view.searchView.setQuery("", false)
                searchKeyWord(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

    }

    override fun onResume() {
        super.onResume()
        refreshSearchView()
    }

    private fun refreshSearchView() {
        requireView().searchList.visibility = if (requireView().searchView.hasFocus()) View.VISIBLE else View.GONE
        Logger.d("refreshSearchView:${requireView().searchView.hasFocus()}\n" +
                "searchViewLayout.visibility:${requireView().searchList.visibility}")
    }

    private fun searchKeyWord(query: String?) {
        query?.run {
            ImmUtils.hideImm(requireActivity())
            val bundle = bundleOf(Pair(CommonConstants.KEYS.SEARCH_ARGS, this))
            goMoreFragment(requireView(), CommonConstants.Action.SEARCH, bundle)
            requireView().searchList.visibility = View.GONE
        }
    }

    @Synchronized
    private fun goMoreFragment(view: View, action: Int, bundle: Bundle? = null) {
        val args = Bundle()
        args.putInt(CommonConstants.Action.KEY, action)
        bundle?.let {
            args.putAll(it)
        }
        view.findNavController().navigate(R.id.action_mainIndexFragment_to_moreFragment, args)
    }

    //DispatchNestedScrollView是否需要拦截子View的滑动事件
    private fun isNeedIntercept(ev: MotionEvent?, direction: Int): Boolean {
        ev?.let {
            view?.postRecyclerView?.let {
                if (Utils.isInViewScope(it, ev.rawX, ev.rawY)
                        && (0 >= (view!!.dividerAfterHotImg!!.y - view!!.nestedScrollView!!.scrollY))
                        && direction == DispatchNestedScrollView.Companion.Direction.VERTICAL) {
                    return false
                }
            }
            view?.banner?.let {
                if (Utils.isInViewScope(it, ev.rawX, ev.rawY)
                        && direction == DispatchNestedScrollView.Companion.Direction.HORIZONTAL) {
                    return false
                }
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main_index, menu)
        val searchManager = requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView: SearchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
    }

}