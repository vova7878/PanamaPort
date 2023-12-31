package com.v7878.unsafe.foreign;

import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// see elf.h
@SuppressWarnings("Since15")
class ELF {
    public static final int EI_NIDENT = 16;
    public static final byte[] ELFMAG = {0x7f, 'E', 'L', 'F'};

    public static final int STT_FUNC = 2;

    public static final ValueLayout Elf32_Addr = JAVA_INT;
    public static final ValueLayout Elf32_Half = JAVA_SHORT;
    public static final ValueLayout Elf32_Off = JAVA_INT;
    public static final ValueLayout Elf32_Word = JAVA_INT;

    public static final ValueLayout Elf64_Addr = JAVA_LONG;
    public static final ValueLayout Elf64_Half = JAVA_SHORT;
    public static final ValueLayout Elf64_Off = JAVA_LONG;
    public static final ValueLayout Elf64_Word = JAVA_INT;
    public static final ValueLayout Elf64_Xword = JAVA_LONG;

    public static final GroupLayout Elf32_Ehdr = structLayout(
            sequenceLayout(EI_NIDENT, JAVA_BYTE).withName("e_ident"),
            Elf32_Half.withName("e_type"),
            Elf32_Half.withName("e_machine"),
            Elf32_Word.withName("e_version"),
            Elf32_Addr.withName("e_entry"),
            Elf32_Off.withName("e_phoff"),
            Elf32_Off.withName("e_shoff"),
            Elf32_Word.withName("e_flags"),
            Elf32_Half.withName("e_ehsize"),
            Elf32_Half.withName("e_phentsize"),
            Elf32_Half.withName("e_phnum"),
            Elf32_Half.withName("e_shentsize"),
            Elf32_Half.withName("e_shnum"),
            Elf32_Half.withName("e_shstrndx")
    );
    public static final GroupLayout Elf64_Ehdr = structLayout(
            sequenceLayout(EI_NIDENT, JAVA_BYTE).withName("e_ident"),
            Elf64_Half.withName("e_type"),
            Elf64_Half.withName("e_machine"),
            Elf64_Word.withName("e_version"),
            Elf64_Addr.withName("e_entry"),
            Elf64_Off.withName("e_phoff"),
            Elf64_Off.withName("e_shoff"),
            Elf64_Word.withName("e_flags"),
            Elf64_Half.withName("e_ehsize"),
            Elf64_Half.withName("e_phentsize"),
            Elf64_Half.withName("e_phnum"),
            Elf64_Half.withName("e_shentsize"),
            Elf64_Half.withName("e_shnum"),
            Elf64_Half.withName("e_shstrndx")
    );
    public static final GroupLayout Elf_Ehdr = IS64BIT ? Elf64_Ehdr : Elf32_Ehdr;

    public static final GroupLayout Elf32_Sym = structLayout(
            Elf32_Word.withName("st_name"),
            Elf32_Addr.withName("st_value"),
            Elf32_Word.withName("st_size"),
            JAVA_BYTE.withName("st_info"),
            JAVA_BYTE.withName("st_other"),
            Elf32_Half.withName("st_shndx")
    );
    public static final GroupLayout Elf64_Sym = structLayout(
            Elf64_Word.withName("st_name"),
            JAVA_BYTE.withName("st_info"),
            JAVA_BYTE.withName("st_other"),
            Elf64_Half.withName("st_shndx"),
            Elf64_Addr.withName("st_value"),
            Elf64_Xword.withName("st_size")
    );
    public static final GroupLayout Elf_Sym = IS64BIT ? Elf64_Sym : Elf32_Sym;

    public static final GroupLayout Elf32_Shdr = structLayout(
            Elf32_Word.withName("sh_name"),
            Elf32_Word.withName("sh_type"),
            Elf32_Word.withName("sh_flags"),
            Elf32_Addr.withName("sh_addr"),
            Elf32_Off.withName("sh_offset"),
            Elf32_Word.withName("sh_size"),
            Elf32_Word.withName("sh_link"),
            Elf32_Word.withName("sh_info"),
            Elf32_Word.withName("sh_addralign"),
            Elf32_Word.withName("sh_entsize")
    );
    public static final GroupLayout Elf64_Shdr = structLayout(
            Elf64_Word.withName("sh_name"),
            Elf64_Word.withName("sh_type"),
            Elf64_Xword.withName("sh_flags"),
            Elf64_Addr.withName("sh_addr"),
            Elf64_Off.withName("sh_offset"),
            Elf64_Xword.withName("sh_size"),
            Elf64_Word.withName("sh_link"),
            Elf64_Word.withName("sh_info"),
            Elf64_Xword.withName("sh_addralign"),
            Elf64_Xword.withName("sh_entsize")
    );
    public static final GroupLayout Elf_Shdr = IS64BIT ? Elf64_Shdr : Elf32_Shdr;

    public static class Element {

        public final String name;
        public final ByteBuffer data;

        public Element(String name, ByteBuffer data) {
            this.name = name;
            this.data = data;
        }
    }

    private static final int sh_offset = (int) Elf_Shdr.byteOffset(
            groupElement("sh_offset"));
    private static final int sh_size = (int) Elf_Shdr.byteOffset(
            groupElement("sh_size"));

    private static final int e_shentsize = (int) Elf_Ehdr.byteOffset(
            groupElement("e_shentsize"));
    private static final int e_shnum = (int) Elf_Ehdr.byteOffset(
            groupElement("e_shnum"));
    private static final int e_shoff = (int) Elf_Ehdr.byteOffset(
            groupElement("e_shoff"));
    private static final int e_shstrndx = (int) Elf_Ehdr.byteOffset(
            groupElement("e_shstrndx"));

    private static final int sh_name = (int) Elf_Shdr.byteOffset(
            groupElement("sh_name"));

    private static final int st_name = (int) Elf_Sym.byteOffset(
            groupElement("st_name"));
    private static final int st_info = (int) Elf_Sym.byteOffset(
            groupElement("st_info"));
    private static final int st_value = (int) Elf_Sym.byteOffset(
            groupElement("st_value"));
    private static final int st_size = (int) Elf_Sym.byteOffset(
            groupElement("st_size"));

    private static ByteBuffer slice(ByteBuffer bb, int start, int size) {
        int old_pos = bb.position();
        int old_lim = bb.limit();
        bb.position(start);
        bb.limit(start + size);
        ByteBuffer out = bb.slice().order(bb.order());
        bb.limit(old_lim);
        bb.position(old_pos);
        return out;
    }

    private static long getWord(ByteBuffer bb, int pos) {
        return IS64BIT ? bb.getLong(pos) : bb.getInt(pos) & 0xffffffffL;
    }

    private static int strlen(ByteBuffer bb, int pos) {
        int i = 0;
        for (; i < bb.remaining(); i++) {
            if (bb.get(i + pos) == 0) {
                break;
            }
        }
        return i;
    }

    private static String getCString(ByteBuffer bb, int pos) {
        int length = strlen(bb, pos);
        byte[] data = new byte[length];
        int old_pos = bb.position();
        bb.position(pos);
        bb.get(data);
        bb.position(old_pos);
        return new String(data);
    }

    public static ByteBuffer getRawSegmentData(ByteBuffer in, ByteBuffer segment) {
        long off = getWord(segment, sh_offset);
        long size = getWord(segment, sh_size);
        return slice(in, (int) off, (int) size);
    }

    public static class SymTab {

        public final Map<String, Element> dyn = new HashMap<>();
        public final Map<String, Element> sym = new HashMap<>();

        public Element find(String name) {
            Objects.requireNonNull(name);
            Element out = dyn.get(name);
            if (out == null) {
                out = sym.get(name);
            }
            if (out == null) {
                throw new IllegalArgumentException("symbol '" + name + "' not found");
            }
            return out;
        }

        public MemorySegment findFunction(String name, long bias) {
            ByteBuffer symbol = find(name).data;
            int type = symbol.get(st_info) & 0xf;
            if (type != STT_FUNC) {
                throw new IllegalArgumentException("unknown symbol type: " + type);
            }
            long value = getWord(symbol, st_value);
            long size = getWord(symbol, st_size);
            return MemorySegment.ofAddress(bias + value).reinterpret(size);
        }
    }

    public static void readSymbols(ByteBuffer in, ByteBuffer symtab, ByteBuffer strtab,
                                   Map<String, Element> out) {

        symtab = getRawSegmentData(in, symtab);
        strtab = getRawSegmentData(in, strtab);

        if (symtab.capacity() % Elf_Sym.byteSize() != 0) {
            throw new IllegalArgumentException("elf error");
        }
        int sym_size = (int) Elf_Sym.byteSize();
        int num = symtab.capacity() / sym_size;
        for (int i = 0; i < num; i++) {
            ByteBuffer symbol = slice(symtab, i * sym_size, sym_size);
            int name_off = symbol.getInt(st_name);
            String name = getCString(strtab, name_off);
            out.put(name, new Element(name, symbol));
        }
    }

    public static Element[] readSegments(ByteBuffer in) {
        ByteBuffer ehdr = slice(in, 0, (int) Elf_Ehdr.byteSize());
        if (ehdr.get(0) != ELFMAG[0]
                || ehdr.get(1) != ELFMAG[1]
                || ehdr.get(2) != ELFMAG[2]
                || ehdr.get(3) != ELFMAG[3]) {
            throw new IllegalArgumentException("not an elf");
        }
        int shentsize = ehdr.getChar(e_shentsize);
        if (Elf_Shdr.byteSize() != shentsize) {
            throw new IllegalArgumentException("elf error: e_shentsize(="
                    + shentsize + ") != sizeof(Elf_Shdr)(=" + Elf_Shdr.byteSize() + ")");
        }
        int shnum = ehdr.getChar(e_shnum);
        int shoff = (int) getWord(ehdr, e_shoff);
        ByteBuffer all_sh = slice(in, shoff, shnum * shentsize);
        int shstrndx = ehdr.getChar(e_shstrndx);
        ByteBuffer strings = getRawSegmentData(in, slice(all_sh, shstrndx * shentsize, shentsize));
        Element[] segments = new Element[shnum];
        for (int i = 0; i < shnum; i++) {
            ByteBuffer segment = slice(all_sh, i * shentsize, shentsize);
            int name_off = segment.getInt(sh_name);
            String name = getCString(strings, name_off);
            segments[i] = new Element(name, segment);
        }
        return segments;
    }

    public static SymTab readSymTab(ByteBuffer in, boolean only_dyn) {
        Element[] segments = readSegments(in);

        Element symtab = null;
        Element strtab = null;
        Element dynsym = null;
        Element dynstr = null;

        for (Element segment : segments) {
            switch (segment.name) {
                case ".strtab":
                    if (!only_dyn) {
                        if (strtab != null) {
                            throw new IllegalArgumentException(
                                    "too many string tables");
                        }
                        strtab = segment;
                    }
                    break;
                case ".symtab":
                    if (!only_dyn) {
                        if (symtab != null) {
                            throw new IllegalArgumentException(
                                    "too many symbol tables");
                        }
                        symtab = segment;
                    }
                    break;
                case ".dynstr":
                    if (dynstr != null) {
                        throw new IllegalArgumentException(
                                "too many string tables");
                    }
                    dynstr = segment;
                    break;
                case ".dynsym":
                    if (dynsym != null) {
                        throw new IllegalArgumentException(
                                "too many symbol tables");
                    }
                    dynsym = segment;
                    break;
            }
        }

        SymTab out = new SymTab();

        if ((symtab != null) && (strtab != null)) {
            readSymbols(in, symtab.data, strtab.data, out.sym);
        }
        if ((dynsym != null) && (dynstr != null)) {
            readSymbols(in, dynsym.data, dynstr.data, out.dyn);
        }

        return out;
    }
}
