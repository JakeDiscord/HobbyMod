package io.github.jakediscord.hobbymod.dj.client;

/** Child editors must route asynchronous save acknowledgements back to the active workstation. */
interface DjEditorOverlay {
    DjScreen editor();
}
