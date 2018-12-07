import java.util.PriorityQueue;

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		int [] counts= readForCounts(in);
		HuffNode root= makeTreeFromCounts(counts);
		String [] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);
		
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
	}
	private void writeCompressedBits(String[] encods, BitInputStream in, BitOutputStream out) {
		// TODO Auto-generated method stub
		while(true) {
			int bit = in.readBits(BITS_PER_WORD);
			if (bit==-1) break;
			else {
				String cod= encods[bit];
				out.writeBits(cod.length(), Integer.parseInt(cod, 2));
			}
		}
		String cod= encods[PSEUDO_EOF];
		out.writeBits(cod.length(), Integer.parseInt(cod, 2));
		
	}

	private void writeHeader(HuffNode root, BitOutputStream out) {
		// TODO Auto-generated method stub
		if(root==null) return;
		if (root.myLeft==null || root.myRight==null) {
			out.writeBits(1, 0);
			writeHeader(root.myLeft, out);
			writeHeader(root.myRight, out);
		}else {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD+1, root.myValue);
			return;
		}
		
	}

	private String[] makeCodingsFromTree(HuffNode root) {
		// TODO Auto-generated method stub
		String[] encods= new String[ALPH_SIZE + 1];
		codingHelper(root, "", encods);
		
		return encods;
	}

	private void codingHelper(HuffNode root, String string, String[] encods) {
		// TODO Auto-generated method stub
		if (root.myLeft==null && root.myRight==null) {
			encods[root.myValue]=string;
			return;
		}
		//recursion
		codingHelper(root.myLeft, string + "0", encods);
		codingHelper(root.myRight, string + "0", encods);
		
	}

	private HuffNode makeTreeFromCounts(int[] counts) {
		// TODO Auto-generated method stub
	PriorityQueue<HuffNode> pq= new PriorityQueue<>();
	
	for (int i=0; i<counts.length; i++) {
		if (counts[i] > 0) pq.add(new HuffNode(i, counts[i], null, null));
	}
	while (pq.size() > 1) {
		HuffNode left= pq.remove();
		HuffNode right= pq.remove();
		HuffNode x= new HuffNode(0, left.myWeight + right.myWeight, left, right);
		pq.add(x);
	}
	HuffNode r= pq.remove();
		return r;
	}

	private int[] readForCounts(BitInputStream in) {
		int [] vals = new int[ALPH_SIZE + 1];
		vals[PSEUDO_EOF]=1;
		while(true) {
			int b = in.readBits(BITS_PER_WORD);
			if (b==-1) break;
			vals[b]++;
			
		}
		return vals;
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	
	
	public void decompress(BitInputStream in, BitOutputStream out){
		int bits= in.readBits(BITS_PER_INT);
		
		
		//two exceptions done
		if(bits!= HUFF_TREE) throw new HuffException("wrong header, starts with" +bits);
		
	
		HuffNode root= readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();
	}

	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		// TODO Auto-generated method stub
		HuffNode cur = root;
		while(true) {
			int b = in.readBits(1);
			if(b == -1) throw new HuffException("wrong input");	
			if (b==0) cur=cur.myLeft;
			else cur= cur.myRight;
			if (cur.myRight==null && cur.myLeft==null) {
				if (cur.myValue == PSEUDO_EOF) break;
				else {
					out.writeBits(BITS_PER_WORD, cur.myValue);
					cur= root;
				}
				
			}
		}
		
	}

	private HuffNode readTreeHeader(BitInputStream in) {
		// TODO Auto-generated method stub
		int b = in.readBits(1);
		if(b == -1) throw new HuffException("wrong input");
		if(b == 0) { //repeat process until this no longer equals 0
			HuffNode left = readTreeHeader(in);
			HuffNode right= readTreeHeader(in);
			return new HuffNode(0, 0, left, right);
		}else { 
			//end
			int val = in.readBits(BITS_PER_WORD+1);
			return new HuffNode(val, 0, null, null);
		}
		
		
		
		
		
	}
}