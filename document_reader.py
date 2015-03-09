# /usr/bin/python
# -*- coding: utf-8 -*-

"""
This program is to:

"""
import collections
import os
import pprint
import re
import sys

reload(sys)
sys.setdefaultencoding('utf8')

__author__ = 'krim'
__date__ = '3/5/15'
__email__ = 'krim@brandeis.edu'


import main

class reader(object):
    """
    reader is the main class for reading document files in project dataset
    """
    def __init__(self, filename):
        super(reader, self).__init__()
        self.filename = filename
        self.tokenized_sents = []
        self.depparsed_sents = []
        self.process_file()

    def process_file(self):
        self.tokenized_sents = self.tokenize_sent()
        # self.depparsed_sents = self.load_dep_parse()

    def tokenize_sent(self):
        sents = []
        with open(os.path.join(main.POS_DATA_PATH, self.filename + ".raw.pos")) as document:
            for line in document:
                if line != "\n":
                    sents.append(self.tokenize(line))
        return sents

    def get_all_sents(self):
        sents = []
        for sent in self.tokenized_sents:
            sents.append([w for w, _ in sent])
        return sents

    def write_raw_sents(self):
        with open(os.path.join(main.RAW_DATA_PATH, self.filename), "w") as outf:
            for sent in self.get_all_sents():
                outf.write(" ".join(sent))
                outf.write("\n")

    def load_dep_parse(self):
        sents = []
        with open(os.path.join(main.DEPPARSE_DATA_PATH,
                               self.filename + ".raw.depparse")) as parse:
            sent = {}
            for line in parse:
                if line == "\n":
                    sents.append(sent)
                    sent = {}
                else:
                    m = re.match(r"^(.+)\((.+)-([0-9']+), (.+)-([0-9']+)\)", line)
                    if m is None:
                        print "REGEX ERROR: ", line
                        continue
                    rel = m.groups()[0]
                    gov = m.groups()[1]
                    gov_idx = m.groups()[2]
                    if gov_idx.endswith("'"):
                        gov_idx = gov_idx[:-1]
                    gov_idx = int(gov_idx) - 1
                    dep = m.groups()[3]
                    dep_idx = m.groups()[4]
                    if dep_idx.endswith("'"):
                        dep_idx = dep_idx[:-1]
                    dep_idx = int(dep_idx) - 1

                    try:
                        sent[gov_idx][1][rel].append((dep_idx, dep))
                    except KeyError:
                        sent[gov_idx] = (gov,
                                         collections.defaultdict(list),
                                         collections.defaultdict(list))
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
        if not isinstance(sent, int):
            sent = int(sent)
        if not isinstance(start, int):
            start = int(start)
        if not isinstance(end, int):
            end = int(end)
        return self.tokenized_sents[sent][start:end]

    def get_words(self, sent, start, end):
        return [w for w, _ in self.get_tokens(sent, start, end)]

    def get_pos(self, sent, start, end):
        return [p for _, p in self.get_tokens(sent, start, end)]

    def get_dependents(self, sent, start, end):
        return self.depparsed_sents[sent]

if __name__ == '__main__':
    r = reader("APW20001001.2021.0521.head.coref")
    pprint.pprint(r.depparsed_sents)

