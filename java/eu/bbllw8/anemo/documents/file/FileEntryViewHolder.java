/*
 * Copyright (c) 2021 2bllw8
 * SPDX-License-Identifier: GPL-3.0-only
 */
package eu.bbllw8.anemo.documents.file;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

final class FileEntryViewHolder extends RecyclerView.ViewHolder {
    @NonNull
    private final ImageView fileIcon;
    @NonNull
    private final TextView fileDisplayName;

    public FileEntryViewHolder(@NonNull View itemView) {
        super(itemView);
        fileIcon = itemView.findViewById(R.id.fileItemIcon);
        fileDisplayName = itemView.findViewById(R.id.fileItemDisplayName);
    }

    public void bind(@NonNull FileEntry fileEntry) {
        fileIcon.setVisibility(fileEntry.isDirectory()
                ? View.VISIBLE : View.GONE);
        fileDisplayName.setText(fileEntry.getDisplayName());
    }
}
