package edu.cmu.group08.p2pcarpool.group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class GroupContent {

    /**
     * An array of sample (dummy) items.
     */
    public static List<Group> ITEMS = new ArrayList<Group>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static Map<Integer, Group> ITEM_MAP = new HashMap<>();

    static {
        // Add 3 sample items.
//        addItem(new Group(1, "Item 1"));
//        addItem(new Group(2, "Item 2"));
//        addItem(new Group(3, "Item 3"));
    }

    public static void addItem(Group item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }
    public static void removeItem(int id) {
        for (Iterator<Group> iterator = ITEMS.iterator(); iterator.hasNext();) {
            Group g = iterator.next();
            if (g.id == id) {
                iterator.remove();
                break;
            }
        }
        ITEM_MAP.remove(id);
    }
    public static void clearAll() {
        ITEMS.clear();
        ITEM_MAP.clear();
    }
    /**
     * A dummy item representing a piece of content.
     */
    public static class Group {
        public Integer id;
        public String content;

        public Group(int id, String content) {
            this.id = id;
            this.content = content;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
