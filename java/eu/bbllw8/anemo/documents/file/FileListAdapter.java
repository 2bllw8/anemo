/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.file;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

final class FileListAdapter extends RecyclerView.Adapter<FileEntryViewHolder> {
    @NonNull
    private final Consumer<FileEntry> visitEntry;
    @NonNull
    private List<FileEntry> data = Collections.emptyList();

    public FileListAdapter(@NonNull Consumer<FileEntry> visitEntry) {
        this.visitEntry = visitEntry;
    }

    @NonNull
    @Override
    public FileEntryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                  int i) {
        return new FileEntryViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.file_list_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FileEntryViewHolder vh,
                                 int i) {
        final FileEntry item = data.get(i);
        vh.bind(item);
        vh.itemView.setOnClickListener(v -> visitEntry.accept(item));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setData(@NonNull List<FileEntry> newData) {
        final int oldSize = data.size();
        final int newSize = newData.size();

        data = Collections.unmodifiableList(newData);
        if (oldSize == 0) {
            notifyItemRangeInserted(0, newSize);
        } else {
            int diff = oldSize - newSize;
            if (diff > 0) {
                // Items were removed
                notifyItemRangeChanged(0, newSize);
                notifyItemRangeRemoved(newSize, diff);
            } else if (diff < 0) {
                // Items were added
                notifyItemRangeChanged(0, oldSize);
                notifyItemRangeInserted(oldSize, diff * -1);
            } else {
                notifyItemChanged(0, oldSize);
            }
        }
    }
}
