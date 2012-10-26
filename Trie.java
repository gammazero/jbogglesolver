/**
 * An implementation of a trie that supports approximate matching of words
 * using edit distance and word frequency.
 * 
 * This trie implementation is not thread-safe.
 * 
 * @author Andrew Gillis
 */
public class Trie {

    private static final int ALPH = 26;
    private Trie[] children;
    private boolean isWord;

    public Trie() {
        children = new Trie[ALPH];
        isWord = false;
    }

    public void insert(String word) {
        word = word.toLowerCase();
        Trie t = this;
        int k;
        int limit = word.length();
        for(k=0; k < limit; k++) {
            int index = word.charAt(k) - 'a';
            if (t.children[index] == null) {
                t.children[index] = new Trie();
            }
            t = t.children[index];
        }
        t.isWord = true;
    }

    public boolean contains(String s) {
        s = s.toLowerCase();
        Trie t = this;
        int k;
        int limit = s.length();
        for(k=0; k < limit; k++) {
            int index = s.charAt(k) - 'a';
            t = t.children[index];
            if (t == null) {
                return false;
            }
        }
        return t.isWord;
    }

    public boolean ifIsWord() {
        return isWord;
    }

    public Trie getChild(char c) {
        int index = c - 'a';
        return children[index];
    }
}
