PACKAGENAME = MEAPsoft
VERSION = 1.1.1

SHELL = /bin/sh

JARFILE = $(PACKAGENAME).jar

# directories
SRCDIR = src
BASEPKGDIR = com/meapsoft
METADIR = meta
JARDIR = bin
BINDIR = bin
TMPDIR = tmp
DATADIR = data

# data files to include in the distribution tarball:
DATAFILES = $(DATADIR)/chris_mann.wav

classpath = $(JARDIR)/getopt-1.0.12.jar:$(JARDIR)/tritonus_misc.jar:$(JARDIR)/tritonus_share.jar:$(SRCDIR)

JFLAGS = -classpath $(classpath) -sourcepath $(SRCDIR)

classes = Segmenter.java FeatExtractor.java Synthesizer.java


# fake targets that don't have any dependencies:
.PHONY : all doc dist clean distclean packageclean version

all: jar

%.java: $(SRCDIR)/$(BASEPKGDIR)/%.java
	javac $(JFLAGS) $<

composers: 
	javac $(JFLAGS) $(SRCDIR)/$(BASEPKGDIR)/composers/*.java  

# make sure that the featextractors/distance metrics get compiled
# since no java files depend on all of them directly anymore
classes: $(classes)
	javac $(JFLAGS) $(SRCDIR)/$(BASEPKGDIR)/*Dist.java
	javac $(JFLAGS) $(SRCDIR)/$(BASEPKGDIR)/*/*.java

jar: classes
	-mkdir $(BINDIR)
	jar cfm $(BINDIR)/$(JARFILE) $(METADIR)/manifest.txt -C $(SRCDIR) .

distclean: clean packageclean

clean:
	rm -f $(SRCDIR)/$(BASEPKGDIR)/*class $(SRCDIR)/$(BASEPKGDIR)/*/*class $(SRCDIR)/*/*class

packageclean: 
	rm -fr $(BINDIR)/$(JARFILE)


# This target is not very portable and probably won't work on your
# machine.  It depends on darcs.
dist: jar
	cp $(BINDIR)/$(JARFILE) $(BINDIR)/$(PACKAGENAME)-$(VERSION).jar
	darcs dist -d $(PACKAGENAME)-$(VERSION)
	-mkdir $(TMPDIR);  cd $(TMPDIR);                            \
	mv ../$(PACKAGENAME)-$(VERSION).tar.gz  .;                  \
	tar xzf $(PACKAGENAME)-$(VERSION).tar.gz;                   \
	chmod 755 $(PACKAGENAME)-$(VERSION)/$(BINDIR)/runme.*;      \
	cp -r ../$(BINDIR)/$(JARFILE) $(PACKAGENAME)-$(VERSION)/$(BINDIR); \
	tar czf ../$(PACKAGENAME)-$(VERSION).tar.gz $(PACKAGENAME)-$(VERSION)/*; \
	cd ..;                                                      \
	rm -rf $(TMPDIR)

# Same as above target, just names the tarball MEAPsoft-$version_dev
# instead.  Used for development releases to the MEAP team in between
# major public releases.
dev_dist: jar
	cp $(BINDIR)/$(JARFILE) $(BINDIR)/$(PACKAGENAME)-$(VERSION)_dev.jar
	darcs dist -d $(PACKAGENAME)-$(VERSION)_dev
	-mkdir $(TMPDIR);  cd $(TMPDIR);                            \
	mv ../$(PACKAGENAME)-$(VERSION)_dev.tar.gz  .;                  \
	tar xzf $(PACKAGENAME)-$(VERSION)_dev.tar.gz;                   \
	chmod 755 $(PACKAGENAME)-$(VERSION)_dev/$(BINDIR)/runme.*;      \
	cp -r ../$(BINDIR)/$(JARFILE) $(PACKAGENAME)-$(VERSION)_dev/$(BINDIR); \
	tar czf ../$(PACKAGENAME)-$(VERSION)_dev.tar.gz $(PACKAGENAME)-$(VERSION)_dev/*; \
	cd ..;                                                      \
	rm -rf $(TMPDIR)

# This target is not very portable and probably won't work on your
# machine.  Make sure Doxygen is in your path if you want it to work.
doc: version
	cd doc; doxygen ; cd ..
	-mkdir doc/javadoc; 
	javadoc -sourcepath $(SRCDIR) -d doc/javadoc com.meapsoft com.meapsoft.featextractors com.meapsoft.composers com.meapsoft.gui com.meapsoft.visualizer;

version:
	sed -i 's/^MEAPsoft-.*/MEAPsoft-$(VERSION)/' README  
	sed -i 's/^PROJECT_NUMBER.*=.*/PROJECT_NUMBER = $(VERSION)/' doc/Doxyfile  
