/*
    PopulationDensity Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.PopulationDensity;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScanRegionTaskAdvanced extends AbstractScanRegionTask
{
    private ChunkSnapshot[][] chunks;
    private boolean openNewRegions;

    private static final int CHUNK_SIZE = 16;

    public ScanRegionTaskAdvanced(ChunkSnapshot[][] chunks, boolean openNewRegions)
    {
        this.chunks = chunks;
        this.openNewRegions = openNewRegions;
    }

    @Override
    public void execute() {
        this.setPriority(Thread.MIN_PRIORITY);
        this.start();
    }

    private class Position
    {
        public int x;
        public int y;
        public int z;

        public Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String toString()
        {
            return new StringBuilder()
                .append(this.x)
                .append(" ")
                .append(this.y)
                .append(" ")
                .append(this.z)
                .toString();
        }
    }

    @Override
    public void run()
    {
        ArrayList<String> logEntries = new ArrayList<>();

        //initialize report content
        int woodCount = 0;
        int coalCount = 0;
        int lapisCount = 0;
        int ironCount = 0;
        int goldCount = 0;
        int redstoneCount = 0;
        int emeraldCount = 0;
        int diamondCount = 0;
        int playerBlocks = 0;

        //initialize a new array to track where we've been
        int maxHeight = PopulationDensity.ManagedWorld.getMaxHeight();
        int x, y, z;
        x = y = z = 0;
        boolean[][][] examined = new boolean[this.chunks.length * CHUNK_SIZE][maxHeight][this.chunks.length * CHUNK_SIZE];
        for (x = 0; x < examined.length; x++)
            for (y = 0; y < examined[0].length; y++)
                for (z = 0; z < examined[0][0].length; z++)
                    examined[x][y][z] = false;

        //find a reasonable start position
        Position currentPosition = null;
        for (x = 0; x < examined.length && currentPosition == null; x++)
        {
            for (z = 0; z < examined[0][0].length && currentPosition == null; z++)
            {
                Position position = new Position(x, maxHeight - 1, z);
                if (this.getMaterialAt(position) == Material.AIR)
                {
                    currentPosition = position;
                }
            }
        }

        //set depth boundary
        //if a player has to brave cavernous depths, those resources aren't "easily attainable"
        int min_y = PopulationDensity.instance.minimumRegionPostY - 20;

        //instantiate empty queue
        ConcurrentLinkedQueue<Position> unexaminedQueue = new ConcurrentLinkedQueue<>();

        //mark start position as examined
        try
        {
            assert currentPosition != null;
            examined[currentPosition.x][currentPosition.y][currentPosition.z] = true;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            logEntries.add("Unexpected Exception: " + e.toString());
        }

        //enqueue that start position
        unexaminedQueue.add(currentPosition);

        //as long as there are positions in the queue, keep going
        while (!unexaminedQueue.isEmpty())
        {
            //dequeue a block
            currentPosition = unexaminedQueue.remove();

            //get material
            Material material = this.getMaterialAt(currentPosition);

            //material == null indicates the data is out of bounds (not in the snapshots)
            //in that case, just move on to the next item in the queue
            if (material == null || currentPosition.y < min_y) continue;

            if (!Not_Placed_By_Player_Material.contains(material))
            {
                switch (material)
                {
                    // Check if it's a pass-through-able block
                    case AIR:
                    case CAVE_AIR: // yes this is a thing now in 1.13 ... don't ask me y...
                    case BUBBLE_COLUMN:
                    case OAK_DOOR:
                    case SPRUCE_DOOR:
                    case BIRCH_DOOR:
                    case JUNGLE_DOOR:
                    case ACACIA_DOOR:
                    case DARK_OAK_DOOR:
                    case CHERRY_DOOR:
                    case MANGROVE_DOOR:
                    case OAK_TRAPDOOR:
                    case SPRUCE_TRAPDOOR:
                    case BIRCH_TRAPDOOR:
                    case JUNGLE_TRAPDOOR:
                    case ACACIA_TRAPDOOR:
                    case DARK_OAK_TRAPDOOR:
                    case CHERRY_TRAPDOOR:
                    case MANGROVE_TRAPDOOR:
                    case IRON_DOOR:
                    case IRON_TRAPDOOR:
                    case LADDER:
                    {
                        //make a list of adjacent blocks
                        ConcurrentLinkedQueue<Position> adjacentPositionQueue = new ConcurrentLinkedQueue<>();
    
                        //x + 1
                        adjacentPositionQueue.add(new Position(currentPosition.x + 1, currentPosition.y, currentPosition.z));
    
                        //x - 1
                        adjacentPositionQueue.add(new Position(currentPosition.x - 1, currentPosition.y, currentPosition.z));
    
                        //z + 1
                        adjacentPositionQueue.add(new Position(currentPosition.x, currentPosition.y, currentPosition.z + 1));
    
                        //z - 1
                        adjacentPositionQueue.add(new Position(currentPosition.x, currentPosition.y, currentPosition.z - 1));
    
                        //y + 1
                        adjacentPositionQueue.add(new Position(currentPosition.x, currentPosition.y + 1, currentPosition.z));
    
                        //y - 1
                        adjacentPositionQueue.add(new Position(currentPosition.x, currentPosition.y - 1, currentPosition.z));
    
                        //for each adjacent block
                        while (!adjacentPositionQueue.isEmpty())
                        {
                            Position adjacentPosition = adjacentPositionQueue.remove();
    
                            try
                            {
                                //if it hasn't been examined yet
                                if (!examined[adjacentPosition.x][adjacentPosition.y][adjacentPosition.z])
                                {
                                    //mark it as examined
                                    examined[adjacentPosition.x][adjacentPosition.y][adjacentPosition.z] = true;
    
                                    //shove it in the queue for processing
                                    unexaminedQueue.add(adjacentPosition);
                                }
                            }
    
                            //ignore any adjacent blocks which are outside the snapshots
                            catch (ArrayIndexOutOfBoundsException e) {}
                        }
                        break;
                    }
    
                    // Check if it's a log
                    case OAK_LOG:
                    case SPRUCE_LOG:
                    case BIRCH_LOG:
                    case JUNGLE_LOG:
                    case ACACIA_LOG:
                    case DARK_OAK_LOG:
                    case CHERRY_LOG:
                    case MANGROVE_LOG:
                    {
                        woodCount++;
                        break;
                    }
    
                    // Check if it's an ore
                    case COAL_ORE:
                    case DEEPSLATE_COAL_ORE:
                    {
                        coalCount++;
                        break;
                    }
                    case IRON_ORE:
                    case DEEPSLATE_IRON_ORE:
                    {
                        ironCount++;
                        break;
                    }
                    case GOLD_ORE:
                    case DEEPSLATE_GOLD_ORE:
                    {
                        goldCount++;
                        break;
                    }
                    case REDSTONE_ORE:
                    case DEEPSLATE_REDSTONE_ORE:
                    {
                        redstoneCount++;
                        break;
                    }
                    case LAPIS_ORE:
                    case DEEPSLATE_LAPIS_ORE:
                    {
                        lapisCount++;
                        break;
                    }
                    case EMERALD_ORE:
                    case DEEPSLATE_EMERALD_ORE:
                    {
                        emeraldCount++;
                        break;
                    }
                    case DIAMOND_ORE:
                    case DEEPSLATE_DIAMOND_ORE:
                    {
                        diamondCount++;
                        break;
                    }
                    // Last but not least: if it's a player block
                    default:
                    {
                        playerBlocks++;
                        break;
                    }
                }
            }
        }

        //compute a resource score
        int resourceScore = coalCount * 2 + ironCount * 3 + goldCount * 3 + redstoneCount * 3 + emeraldCount * 3 + diamondCount * 4;

        //due to a race condition, bukkit might say a chunk is loaded when it really isn't.
        //in that case, bukkit will incorrectly report that all of the blocks in the chunk are air
        //strategy: if resource score and wood count are flat zero, the result is suspicious, so wait 5 seconds for chunks to load and start over
        //to avoid an infinite loop in a resource-bare region, maximum ONE repetition

        //deliver report
        logEntries.add(" ");
        logEntries.add("Region Scan Results:");
        logEntries.add(" ");
        logEntries.add("Wood: " + woodCount + "  (Minimum: " + PopulationDensity.instance.woodMinimum + ")");
        logEntries.add("Coal: " + coalCount);
        logEntries.add("Iron: " + ironCount);
        logEntries.add("Gold: " + goldCount);
        logEntries.add("Lapis: " + lapisCount);
        logEntries.add("Redstone: " + redstoneCount);
        logEntries.add("Emerald: " + emeraldCount);
        logEntries.add("Diamond: " + diamondCount);
        logEntries.add("Player Blocks: " + playerBlocks + "  (Maximum: " + (PopulationDensity.instance.densityRatio * 40000) + ")");
        logEntries.add(" ");
        logEntries.add("Resource Score: " + resourceScore + "  (Minimum: " + PopulationDensity.instance.resourceMinimum + ")");
        logEntries.add(" ");

        //if NOT sufficient resources for a good start
        if (resourceScore < PopulationDensity.instance.resourceMinimum || woodCount < PopulationDensity.instance.woodMinimum || playerBlocks > 40000 * PopulationDensity.instance.densityRatio)
        {
            if (resourceScore < PopulationDensity.instance.resourceMinimum || woodCount < PopulationDensity.instance.woodMinimum)
            {
                logEntries.add("Summary: Insufficient near-surface resources to support new players.");
            } else if (playerBlocks > 40000 * PopulationDensity.instance.densityRatio)
            {
                logEntries.add("Summary: Region seems overcrowded.");
            }
        }

        //otherwise
        else
        {
            logEntries.add("Summary: Looks good! This region is suitable for new players.");
            openNewRegions = false;
        }

        //now that we're done, notify the main thread
        ScanResultsTask resultsTask = new ScanResultsTask(logEntries, openNewRegions);

        PopulationDensity.instance.getServer().getScheduler().scheduleSyncDelayedTask(PopulationDensity.instance, resultsTask, 5L);
    }

    private Material getMaterialAt(Position position)
    {
        Material material = null;

        int chunkx = position.x / 16;
        int chunkz = position.z / 16;

        try
        {
            ChunkSnapshot snapshot = this.chunks[chunkx][chunkz];
            material = snapshot.getBlockType(position.x % 16, position.y, position.z % 16);
        }
        catch (IndexOutOfBoundsException e) { }

        return material;
    }
}
