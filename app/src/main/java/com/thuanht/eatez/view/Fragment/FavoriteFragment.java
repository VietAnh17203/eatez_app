package com.thuanht.eatez.view.Fragment;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.thuanht.eatez.Adapter.PostFavouriteAdapter;
import com.thuanht.eatez.LocalData.LocalDataManager;
import com.thuanht.eatez.databinding.FragmentFavoriteBinding;
import com.thuanht.eatez.model.Favourite;
import com.thuanht.eatez.model.Post;
import com.thuanht.eatez.view.Activity.HomeActivity;
import com.thuanht.eatez.view.Activity.PostDetailActivity;
import com.thuanht.eatez.view.Dialog.DialogUtil;
import com.thuanht.eatez.viewModel.FavouriteViewModel;
import com.thuanht.eatez.viewModel.SavePostViewModel;

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {

    private FragmentFavoriteBinding binding;
    private FavouriteViewModel viewModel;
    private SavePostViewModel savePostViewModel;
    private PostFavouriteAdapter adapter;
    private List<Favourite> favouriteList = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading, isLastPage = false;
    private int userid = -1;
    // Define for multi selection
    private ActionMode actionMode;
    private boolean isMultiSelect = false;
    //i created List of int type to store id of data, you can create custom class type data according to your need.
    private List<Integer> selectedIds = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(FavouriteViewModel.class);
        savePostViewModel = new ViewModelProvider(requireActivity()).get(SavePostViewModel.class);
        if(LocalDataManager.getInstance().getUserLogin() != null){
            userid = LocalDataManager.getInstance().getUserLogin().getUserid();
        }
        initUI();
        initData();
        eventHandler();
        return binding.getRoot();
    }

    private void eventHandler() {
        binding.nestedScrollViewFav.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
                LoadMoreData();
            }
        });

        binding.swipeRefreshFavourite.setOnRefreshListener(() -> {
            refreshData();
        });
    }

    private void refreshData(){
        binding.progressRefreshFav.setVisibility(View.VISIBLE);
        favouriteList.clear();
        currentPage = 1;
        viewModel.fetchFavouritePost(userid, 1);
        binding.swipeRefreshFavourite.setRefreshing(false);
    }

    private void LoadMoreData() {
        if (this.isLastPage) {
            binding.progressLoadMoreFavourite.setVisibility(View.GONE);
            return;
        }
        ;
        binding.progressLoadMoreFavourite.setVisibility(View.VISIBLE);
        if (!isLoading) {
            currentPage++;
            viewModel.fetchFavouritePost(userid, currentPage);
        }
    }

    private void initUI() {
        adapter = new PostFavouriteAdapter(favouriteList, requireContext(),
                new PostFavouriteAdapter.OnclickItemListener() {
                    @Override
                    public void onClick(Favourite favourite) {
                        if (favourite != null) {
                            goToPostDetailActivity(Integer.parseInt(favourite.getPostId()));
                        }
                    }

                    @Override
                    public void longClick(Favourite favourite) {
                        // Bạn có thể thêm xử lý multi-select tại đây nếu muốn
                    }
                }
        );

        binding.rcvDishesFavourite.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.rcvDishesFavourite.setAdapter(adapter);

        // Swipe to delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // Không hỗ trợ kéo sắp xếp
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Favourite removedItem = favouriteList.get(position);

                // Xóa khỏi danh sách và DB
                favouriteList.remove(position);
                adapter.notifyItemRemoved(position);
                savePostViewModel.unSavePost(userid, Integer.parseInt(removedItem.getPostId()));

                Snackbar.make(binding.rcvDishesFavourite, "Đã xóa mục yêu thích",
                        Snackbar.LENGTH_LONG).setAction("Hoàn tác", v -> {
                    favouriteList.add(position, removedItem);
                    adapter.notifyItemInserted(position);
                    savePostViewModel.savePost(userid, Integer.parseInt(removedItem.getPostId()));
                }).show();
            }
        });
        itemTouchHelper.attachToRecyclerView(binding.rcvDishesFavourite);

        // Observer để hiển thị Toast khi thành công
        savePostViewModel.getIsUnSaveSuccess().observe(requireActivity(), aBoolean -> {
            if (aBoolean) {
                Toast.makeText(requireContext(), "Đã xóa khỏi mục yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
        savePostViewModel.getIsSaveSuccess().observe(requireActivity(), aBoolean -> {
            if (aBoolean) {
                Toast.makeText(requireContext(), "Đã khôi phục mục yêu thích", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnTryAgain.setOnClickListener(v -> {
            binding.layoutDisconnect.setVisibility(View.GONE);
            binding.swipeRefreshFavourite.setVisibility(View.VISIBLE);
            refreshData();
        });
    }

    public void initData() {
        isLoading = true;
        viewModel.getIsLastPage().observe(requireActivity(), aBoolean -> {
            this.isLastPage = aBoolean;
        });
        viewModel.getIsNetworkDisconnect().observe(getViewLifecycleOwner(), isNetworkDisconnet -> {
            if(isNetworkDisconnet){
                binding.swipeRefreshFavourite.setVisibility(View.GONE);
                binding.layoutDisconnect.setVisibility(View.VISIBLE);
                binding.progressLoadMoreFavourite.setVisibility(View.GONE);
            }
        });
        viewModel.getFavourites().observe(requireActivity(), new Observer<List<Favourite>>() {
            @Override
            public void onChanged(List<Favourite> favourites) {
                binding.progressLoadMoreFavourite.setVisibility(View.VISIBLE);
                if (favourites == null) {
                    binding.tvNoPostSaved.setVisibility(View.VISIBLE);
                    binding.progressLoadMoreFavourite.setVisibility(View.GONE);
                    binding.progressRefreshFav.setVisibility(View.GONE);
                    return;
                }
                if (!favourites.isEmpty()) {
                    if (favouriteList.isEmpty()) {
                        favouriteList.addAll(favourites);
                        adapter.notifyDataSetChanged();
                    } else {
                        int startInsertedIndex = favouriteList.size();
                        favouriteList.addAll(favourites);
                        adapter.notifyItemRangeInserted(startInsertedIndex, favouriteList.size());
                    }
                }
                isLoading = false;
                binding.tvNoPostSaved.setVisibility(View.GONE);
                binding.progressLoadMoreFavourite.setVisibility(View.GONE);
                binding.progressRefreshFav.setVisibility(View.GONE);
            }
        });
        viewModel.fetchFavouritePost(userid, 1);
    }


    private void goToPostDetailActivity(int postid) {
        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("postid", postid);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}