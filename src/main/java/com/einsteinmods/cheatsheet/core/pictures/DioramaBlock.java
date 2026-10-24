package com.einsteinmods.cheatsheet.core.pictures;

/** One block of a diorama at its cell: x east, y up, z south, from the diorama's corner. */
public record DioramaBlock(BlockSpec block, int x, int y, int z) {}
