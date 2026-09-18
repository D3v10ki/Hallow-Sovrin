package com.hollowsovereign.data;

/** Implemented on PlayerEntity via mixin so we can attach {@link PlayerData}. */
public interface PlayerDataAccess {
    PlayerData hollowsovereign$getData();
}
