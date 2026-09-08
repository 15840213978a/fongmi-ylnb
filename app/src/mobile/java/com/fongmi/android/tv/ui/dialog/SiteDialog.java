package com.fongmi.android.tv.ui.dialog;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteDialog extends BaseAlertDialog implements SiteAdapter.OnClickListener {

    private DialogSiteBinding binding;
    private SiteListener listener;
    private SiteAdapter adapter;
    private boolean search;
    private boolean change;
    private String searchText = "";
    private String selectedTag = "";

    public static SiteDialog create() {
        return new SiteDialog();
    }

    public SiteDialog search() {
        search = true;
        return this;
    }

    public SiteDialog change() {
        change = true;
        return this;
    }

    public void show(Fragment fragment) {
        show(fragment.getChildFragmentManager(), null);
        if (fragment instanceof SiteListener) listener = (SiteListener) fragment;
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogSiteBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        adapter = new SiteAdapter(this);
        binding.recycler.setAdapter(adapter);
        adapter.search(search).change(change);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(VodConfig.getHomeIndex()));
        initSearch();
        initTags();
    }

    private void initSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString().trim().toLowerCase();
                filterSites();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void initTags() {
        List<String> tags = extractTags();
        if (tags.isEmpty()) {
            binding.tagScroll.setVisibility(View.GONE);
            return;
        }
        binding.tagScroll.setVisibility(View.VISIBLE);
        binding.tagGroup.removeAllViews();

        Chip allChip = new Chip(requireContext());
        allChip.setText("全部");
        allChip.setCheckable(true);
        allChip.setChecked(true);
        allChip.setOnClickListener(v -> {
            selectedTag = "";
            filterSites();
        });
        binding.tagGroup.addView(allChip);

        for (String tag : tags) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setOnClickListener(v -> {
                selectedTag = chip.isChecked() ? tag : "";
                if (!chip.isChecked()) allChip.setChecked(true);
                else allChip.setChecked(false);
                filterSites();
            });
            binding.tagGroup.addView(chip);
        }
    }

    private List<String> extractTags() {
        Set<String> tagSet = new LinkedHashSet<>();
        Pattern pattern = Pattern.compile("\\[([^\\]]+)]");
        for (Site site : VodConfig.get().getSites()) {
            if (site.isHide()) continue;
            Matcher matcher = pattern.matcher(site.getName());
            while (matcher.find()) {
                tagSet.add(matcher.group(1));
            }
        }
        return new ArrayList<>(tagSet);
    }

    private void filterSites() {
        adapter.filter(searchText, selectedTag);
    }

    @Override
    public void onTextClick(Site item) {
        if (listener != null) listener.setSite(item);
        dismiss();
    }

    @Override
    public void onSearchClick(int position, Site item) {
        item.setSearchable(!item.isSearchable()).save();
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onChangeClick(int position, Site item) {
        item.setChangeable(!item.isChangeable()).save();
        adapter.notifyItemChanged(position);
    }

    @Override
    public boolean onSearchLongClick(Site item) {
        boolean result = !item.isSearchable();
        adapter.getItems().forEach(site -> site.setSearchable(result).save());
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        return true;
    }

    @Override
    public boolean onChangeLongClick(Site item) {
        boolean result = !item.isChangeable();
        adapter.getItems().forEach(site -> site.setChangeable(result).save());
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter.getItemCount() == 0) dismiss();
        else if (ResUtil.isLand(requireContext())) setWidth(0.5f);
    }
}