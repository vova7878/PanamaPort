package com.v7878.unsafe.foreign;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.Utils.shouldNotHappen;

import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.ValueLayout;
import com.v7878.unsafe.DangerLevel;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

// see elf.h
class ELF {
    private static final int EI_NIDENT = 16;
    private static final byte[] ELFMAG = {0x7f, 'E', 'L', 'F'};

    private static final int STT_OBJECT = 1;
    private static final int STT_FUNC = 2;

    private static final ValueLayout Elf32_Addr = JAVA_INT;
    private static final ValueLayout Elf32_Half = JAVA_SHORT;
    private static final ValueLayout Elf32_Off = JAVA_INT;
    private static final ValueLayout Elf32_Word = JAVA_INT;

    private static final ValueLayout Elf64_Addr = JAVA_LONG;
    private static final ValueLayout Elf64_Half = JAVA_SHORT;
    private static final ValueLayout Elf64_Off = JAVA_LONG;
    private static final ValueLayout Elf64_Word = JAVA_INT;
    private static final ValueLayout Elf64_Xword = JAVA_LONG;

    private static final GroupLayout Elf32_Ehdr = structLayout(
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
    private static final GroupLayout Elf64_Ehdr = structLayout(
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
    private static final GroupLayout Elf_Ehdr = IS64BIT ? Elf64_Ehdr : Elf32_Ehdr;

    private static final GroupLayout Elf32_Sym = structLayout(
            Elf32_Word.withName("st_name"),
            Elf32_Addr.withName("st_value"),
            Elf32_Word.withName("st_size"),
            JAVA_BYTE.withName("st_info"),
            JAVA_BYTE.withName("st_other"),
            Elf32_Half.withName("st_shndx")
    );
    private static final GroupLayout Elf64_Sym = structLayout(
            Elf64_Word.withName("st_name"),
            JAVA_BYTE.withName("st_info"),
            JAVA_BYTE.withName("st_other"),
            Elf64_Half.withName("st_shndx"),
            Elf64_Addr.withName("st_value"),
            Elf64_Xword.withName("st_size")
    );
    private static final GroupLayout Elf_Sym = IS64BIT ? Elf64_Sym : Elf32_Sym;

    private static final GroupLayout Elf32_Shdr = structLayout(
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
    private static final GroupLayout Elf64_Shdr = structLayout(
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
    private static final GroupLayout Elf_Shdr = IS64BIT ? Elf64_Shdr : Elf32_Shdr;

    public static class Element {

        public final ByteBuffer data;

        private final ByteBuffer strings;
        private final int name_offset;

        private String cached_name;

        public Element(ByteBuffer data, ByteBuffer strings, int name_offset) {
            this.data = data;
            this.strings = strings;
            this.name_offset = name_offset;
        }

        public String getName() {
            if (cached_name != null) {
                return cached_name;
            }
            return cached_name = getCString(strings, name_offset);
        }

        @Override
        public String toString() {
            return "Element{" +
                    "name='" + getName() + '\'' +
                    ", data=" + data +
                    '}';
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

    private static boolean equalsCString(ByteBuffer bb, int pos, byte[] data) {
        if (bb.remaining() < data.length) {
            return false;
        }
        int old_pos = bb.position();
        try {
            for (int i = 0; i < data.length; i++) {
                byte value = bb.get(i + pos);
                if (value == 0 || value != data[i]) {
                    return false;
                }
            }
            return true;
        } finally {
            bb.position(old_pos);
        }
    }

    private static ByteBuffer getRawSegmentData(ByteBuffer in, ByteBuffer segment) {
        long off = getWord(segment, sh_offset);
        long size = getWord(segment, sh_size);
        return slice(in, (int) off, (int) size);
    }

    public static class SymTab {

        private final Element[] symbols;

        SymTab(Element[] symbols) {
            this.symbols = symbols;
        }

        public Element find(String name) {
            Objects.requireNonNull(name);
            byte[] cname = name.getBytes(StandardCharsets.UTF_8);
            Element out = null;
            for (Element tmp : symbols) {
                if (equalsCString(tmp.strings, tmp.name_offset, cname)) {
                    out = tmp;
                    break;
                }
            }
            if (out == null) {
                throw new IllegalArgumentException("symbol '" + name + "' not found");
            }
            return out;
        }

        @DangerLevel(DangerLevel.VERY_CAREFUL)
        private MemorySegment findSymbol(String name, long bias, int expected_type) {
            ByteBuffer symbol = find(name).data;
            int type = symbol.get(st_info) & 0xf;
            if (type != expected_type) {
                throw new IllegalArgumentException("unknown symbol type: " + type);
            }
            long value = getWord(symbol, st_value);
            long size = getWord(symbol, st_size);
            return MemorySegment.ofAddress(bias + value).reinterpret(size);
        }

        @DangerLevel(DangerLevel.VERY_CAREFUL)
        public MemorySegment findFunction(String name, long bias) {
            return findSymbol(name, bias, STT_FUNC);
        }

        @DangerLevel(DangerLevel.VERY_CAREFUL)
        public MemorySegment findObject(String name, long bias) {
            return findSymbol(name, bias, STT_OBJECT);
        }

        @Override
        public String toString() {
            return "SymTab{" +
                    "symbols=" + Arrays.toString(symbols) +
                    '}';
        }
    }

    private static Element[] readSymbols(ByteBuffer in, ByteBuffer symtab, ByteBuffer strtab) {

        symtab = getRawSegmentData(in, symtab);
        strtab = getRawSegmentData(in, strtab);

        if (symtab.capacity() % Elf_Sym.byteSize() != 0) {
            throw new IllegalArgumentException("elf error");
        }
        int sym_size = Math.toIntExact(Elf_Sym.byteSize());
        int num = symtab.capacity() / sym_size;
        Element[] out = new Element[num];
        for (int i = 0; i < num; i++) {
            ByteBuffer symbol = slice(symtab, i * sym_size, sym_size);
            int name_off = symbol.getInt(st_name);
            out[i] = new Element(symbol, strtab, name_off);
        }
        return out;
    }

    private static Element[] readSections(ByteBuffer in) {
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
        int shoff = Math.toIntExact(getWord(ehdr, e_shoff));
        ByteBuffer all_sh = slice(in, shoff, shnum * shentsize);
        int shstrndx = ehdr.getChar(e_shstrndx);
        ByteBuffer strings = getRawSegmentData(in, slice(all_sh, shstrndx * shentsize, shentsize));
        Element[] segments = new Element[shnum];
        for (int i = 0; i < shnum; i++) {
            ByteBuffer segment = slice(all_sh, i * shentsize, shentsize);
            int name_off = segment.getInt(sh_name);
            segments[i] = new Element(segment, strings, name_off);
        }
        return segments;
    }

    private static final byte[] strtab_name = ".strtab".getBytes(StandardCharsets.UTF_8);
    private static final byte[] symtab_name = ".symtab".getBytes(StandardCharsets.UTF_8);
    private static final byte[] dynstr_name = ".dynstr".getBytes(StandardCharsets.UTF_8);
    private static final byte[] dynsym_name = ".dynsym".getBytes(StandardCharsets.UTF_8);

    public static SymTab readSymTab(ByteBuffer in, boolean dyn_or_sym) {
        Element[] sections = readSections(in);

        if (dyn_or_sym) {
            Element dynsym = null;
            Element dynstr = null;
            for (Element section : sections) {
                if (equalsCString(section.strings, section.name_offset, dynstr_name)) {
                    dynstr = section;
                }
                if (equalsCString(section.strings, section.name_offset, dynsym_name)) {
                    dynsym = section;
                }
                if (dynstr != null && dynsym != null) {
                    break;
                }
            }
            Element[] dyn;
            if ((dynsym != null) && (dynstr != null)) {
                dyn = readSymbols(in, dynsym.data, dynstr.data);
            } else {
                dyn = new Element[0];
            }
            return new SymTab(dyn);
        } else {
            Element symtab = null;
            Element strtab = null;
            for (Element section : sections) {
                if (equalsCString(section.strings, section.name_offset, strtab_name)) {
                    strtab = section;
                }
                if (equalsCString(section.strings, section.name_offset, symtab_name)) {
                    symtab = section;
                }
                if (symtab != null && strtab != null) {
                    break;
                }
            }
            Element[] sym;
            if ((symtab != null) && (strtab != null)) {
                sym = readSymbols(in, symtab.data, strtab.data);
            } else {
                sym = new Element[0];
            }
            return new SymTab(sym);
        }
    }

    public static SymTab readSymTab(String path, boolean dyn_or_sym) {
        byte[] tmp;
        try {
            tmp = Files.readAllBytes(new File(path).toPath());
        } catch (IOException ex) {
            throw shouldNotHappen(ex);
        }
        return readSymTab(ByteBuffer.wrap(tmp).order(ByteOrder.nativeOrder()), dyn_or_sym);
    }
}
