# /usr/bin/python
# -*- coding: utf-8 -*-

"""
This program contains:
File reader for main script (raw text, syntactic parses, dependency parses)
as well as meny helper function for tree features and WordNet features
"""
import collections
import os
import pprint
import re
import sys

from nltk.corpus import wordnet as wn
from nltk.tree import ParentedTree as ptree


reload(sys)
sys.setdefaultencoding('utf8')

__author__ = ["Keigh Rim", "Todd Curcuru", "Yalin Liu"]
__date__ = "3/2/2015"
__email__ = ['krim@brandeis.edu', 'tcurcuru@brandeis.edu', 'yalin@brandeis.edu']

PROJECT_PATH = os.getcwd()
DATA_PATH = os.path.join(PROJECT_PATH, "data")
POS_DATA_PATH = os.path.join(DATA_PATH, "postagged")
RAW_DATA_PATH = os.path.join(DATA_PATH, "rawtext")
DEPPARSE_DATA_PATH = os.path.join(DATA_PATH, "depparsed")
SYNPARSE_DATA_PATH = os.path.join(DATA_PATH, "synparsed")

class reader(object):
    """
    reader is the main class for reading document files in project dataset
    Basically a reader object represent a document in corpus
    """
    def __init__(self, filename):
        super(reader, self).__init__()
        self.filename = filename
        # POS tagged
        self.tokenized_sents = self.tokenize_sent()
        # PS parsing
        self.depparsed_sents = self.load_dep_parse()
        # Dep parsing
        self.synparsed_sents = self.load_syn_parse()

    def tokenize_sent(self):
        """separate out words and POS tags from .postagged files"""
        sents = []
        with open(os.path.join(POS_DATA_PATH,
                               self.filename + ".raw.pos")) as document:
            for line in document:
                # need to be careful about underscored characters
                line = re.sub(r"\b(_+)_([^_])", r"-_\2", line)
                if line != "\n":
                    sents.append(self.tokenize(line))
        return sents

    def get_all_sents(self):
        sents = []
        for sent in self.tokenized_sents:
            sents.append([w for w, _ in sent])
        return sents

    def write_raw_sents(self):
        """write a cleaned out raw text file from a given .postagged file"""
        with open(os.path.join(RAW_DATA_PATH, self.filename + ".raw"), "w") as outf:
            for sentence in self.get_all_sents():
                sent = " ".join(sentence)
                sent = sent.replace("(", "[")
                sent = sent.replace(")", "]")
                outf.write(sent)
                outf.write("\n")

    def load_syn_parse(self):
        """load a string of syntactic parse into NLTK ParentedTree structure"""
        sents = []
        with open(os.path.join(SYNPARSE_DATA_PATH,
                               self.filename + ".raw.psparse")) as parse:
            sent = ""
            for line in parse:
                if line == "\n":
                    try:
                        sents.append(ptree.fromstring(sent))
                        sent = ""
                    except ValueError:
                        print self.filename
                        print sent
                        raise()
                else:
                    sent += line.strip()
        return sents

    def load_dep_parse(self):
        """load a string of Stanford Dependency parse into a data structure"""
        sents = []
        with open(os.path.join(DEPPARSE_DATA_PATH,
                               self.filename + ".raw.depparse")) as parse:
            sent = {}
            for line in parse:
                if line == "\n":
                    sents.append(sent)
                    sent = {}
                else:
                    # each line of Stanford dependency looks like this
                    # relation(govennor-gov_index, dependent-dep_index)
                    m = re.match(r"^(.+)\((.+)-([0-9']+), (.+)-([0-9']+)\)", line)
                    if m is None:
                        print "REGEX ERROR: ", line
                        continue
                    rel = m.groups()[0]
                    gov = m.groups()[1]
                    gov_idx = m.groups()[2]
                    # collapse primed nodes
                    if gov_idx.endswith("'"):
                        gov_idx = gov_idx.replace("'", "")
                    gov_idx = int(gov_idx) - 1
                    dep = m.groups()[3]
                    dep_idx = m.groups()[4]
                    if dep_idx.endswith("'"):
                        dep_idx = dep_idx.replace("'", "")
                    dep_idx = int(dep_idx) - 1

                    # final data structure will be a dict from
                    # token_index: (token_word,
                    #               dict of dependents of this token,
                    #               dict of governors of this token)
                    try:
                        sent[gov_idx][1][rel].append((dep_idx, dep))
                    except KeyError:
                        sent[gov_idx] = (gov,                               # name
                                         collections.defaultdict(list),     # deps
                                         collections.defaultdict(list))     # govs
                        sent[gov_idx][1][rel].append((dep_idx, dep))

                    try:
                        sent[dep_idx][2][rel].append((gov_idx, gov))
                    except KeyError:
                        sent[dep_idx] = (dep,
                                         collections.defaultdict(list),
                                         collections.defaultdict(list))
                        sent[dep_idx][2][rel].append((gov_idx, gov))
        return sents

    @staticmethod
    def tokenize(line):
        """returns [(word, pos)]"""
        tokens = []
        for token in line.split():
            token = token.split("_")
            if len(token) > 2:
                token = ["".join(token[:-1]), token[-1]]
            tokens.append(token)
        # return [tuple(token.split("_")) for token in line.split()]
        return tokens

    def get_tokens(self, sent, start, end):
        """get tokens at particular position of particular sentence"""
        if not isinstance(sent, int):
            sent = int(sent)
        if not isinstance(start, int):
            start = int(start)
        if not isinstance(end, int):
            end = int(end)
        return self.tokenized_sents[sent][start:end]

    def get_words(self, sent, start, end):
        """get words at particular position of particular sentence"""
        return [w for w, _ in self.get_tokens(sent, start, end)]

    def get_pos(self, sent, start, end):
        """get POS tags at particular position of particular sentence"""
        return [p for _, p in self.get_tokens(sent, start, end)]

    def get_dependents(self, sent):
        """get dependency tree of particula sentnece"""
        return self.depparsed_sents[sent]

    def compute_tree_path(self, sent, from_node, to_node):
        """
        compute distance between two nodes in NLTK tree,
        It's using NLTK methods, because we love NLTK
        """
        tree = self.synparsed_sents[sent]
        lowest_descendant = tree.treeposition_spanning_leaves(from_node, to_node)
        upward_path_length = len(tree.leaf_treeposition(from_node))\
                             - len(lowest_descendant)
        downward_path_length = len(tree.leaf_treeposition(to_node)) \
                               - len(lowest_descendant)
        return upward_path_length, downward_path_length

    def get_dep_relation(self, sent, from_node, to_node):
        """get dependency relation of two nodes"""
        parse = self.depparsed_sents[sent]
        # in case a mention is a relative pronoun go back to its antecedent
        while not parse.get(from_node):
            from_node -= 1
        while not parse.get(to_node):
            to_node -= 1

        # bi-directional check: i can govern j as well as i can be depending on j
        dependents = parse[from_node][1]
        for rel, dependent in dependents.iteritems():
            if dependent[0][0] == to_node:
                return rel
        governors = parse[from_node][2]
        for rel, governor in governors.iteritems():
            if governor[0][0] == to_node:
                return rel
        # if no relation found, return null
        return

    def is_subject(self, sent, token_offset):
        """return true if a token is playing subject"""
        parse = self.depparsed_sents[sent]

        # since we are using collapsed dep parse trees,
        # in case a mention is a relative pronoun, it is collapsed in the parse
        # Thus, need to track backward for its antecedent
        while not parse.get(token_offset):
            token_offset -= 1

        governors = parse[token_offset][2]
        if "nsubj" in governors.keys() \
                or "nsubjpass" in governors.keys() \
                or "subj" in governors.keys():
            return True
        else:
            return False

    def is_object(self, sent, token_offset):
        """return true if a token is playing object"""
        parse = self.depparsed_sents[sent]

        # for rel_pronuon
        while not parse.get(token_offset):
            token_offset -= 1
        governors = parse[token_offset][2]
        if "dobj" in governors.keys() \
                or "iobj" in governors.keys() \
                or "obj" in governors.keys():
            return True
        else:
            return False

    def get_deprel_verb(self, sent, noun_offset):
        """get a NE's governing verb is it's relation to its verb"""
        parse = self.depparsed_sents[sent]

        # can this go into infinite loop? NO
        while True:
            # rel_pronoun
            while not parse.get(noun_offset):
                noun_offset -= 1
            # go through the list of governors
            governors = parse[noun_offset][2]
            for num, rel in enumerate(governors.keys()):
                # if verb found, return role and verb
                if rel in ("subj", "nsubj", "nsubjpass"):
                    return "subj", governors[rel][0][1]
                elif rel in ("obj", "dobj", "iobj"):
                    return "obj", governors[rel][0][1]
                # if current token has a noun governor, move to that one,
                # continue to go up the tree
                elif rel in ("appos", "nn", "poss", "conj_and", "conj_or"):
                    new_noun_offset = governors[rel][0][0]
                    if new_noun_offset != noun_offset:
                        noun_offset = new_noun_offset
                        break
                    else:
                        continue
                return None, None


    @staticmethod
    def in_same_synsets(verb_k, verb_t):
        """query WordNet to see two verbs are in same synset"""
        if verb_k is None or verb_t is None:
            return False
        else:
            verbset = set()
            for synset in wn.synsets(verb_k):
                verbset.update(synset.lemma_names())
            if verb_t in verbset:
                return True
            else:
                verbset = set()
                for synset in wn.synsets(verb_t):
                    verbset.update(synset.lemma_names())
                return verb_k in verbset


if __name__ == '__main__':
    # for filename in os.listdir(POS_DATA_PATH):
    #     filename = filename[:-8]
    #     r = reader(filename)
    #     r.write_raw_sents()

    r = reader("NYT20001230.1309.0093.head.coref")
    # print r.is_subject(15,4)
    # print r.is_object(15,4)
    print r.get_dep_relation(17, 6, 10)
    print r.in_same_synsets("said", "eat")

