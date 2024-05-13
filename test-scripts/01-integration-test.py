import argparse
import json
import unittest
import urllib3


def parse_args():
    p = argparse.ArgumentParser(
        description="runs tests migrated from :integraion")
    p.add_argument("--port", type=int, default=9200)
    p.add_argument("--host", default="http://localhost")
    p.add_argument("--index", default="test_sudachi")
    return p.parse_args()


def main():
    global es_instance
    es_instance = ElasticSearch(parse_args())
    unittest.main()
    return


es_instance = None


class ElasticSearch(object):
    def __init__(self, args):
        self.base_url = f"{args.host}:{args.port}"
        self.index_url = f"{self.base_url}/{args.index}"
        self.mgr = urllib3.PoolManager()

    def analyze(self, body):
        r = self.mgr.urlopen(
            "GET",
            f"{self.base_url}/_analyze",
            headers={"Content-Type": "application/json"},
            body=json.dumps(body),
        )
        return r


class TestBasic(unittest.TestCase):
    def test_can_instanciate_sudachi_analyzer(self):
        body = {"analyzer": "sudachi", "text": ""}
        resp = es_instance.analyze(body)
        self.assertEqual(200, resp.status)
        return

    def test_tokenize_using_sudachi_tokenizer(self):
        body = {"tokenizer": "sudachi_tokenizer", "text": "京都に行った"}
        resp = es_instance.analyze(body)
        self.assertEqual(200, resp.status)

        tokens = json.loads(resp.data)["tokens"]
        self.assertEqual(4, len(tokens))
        self.assertEqual("京都", tokens[0]["token"])
        self.assertEqual(0, tokens[0]["position"])
        self.assertEqual(0, tokens[0]["start_offset"])
        self.assertEqual(2, tokens[0]["end_offset"])

        self.assertEqual("に", tokens[1]["token"])
        self.assertEqual(1, tokens[1]["position"])
        self.assertEqual(2, tokens[1]["start_offset"])
        self.assertEqual(3, tokens[1]["end_offset"])

        self.assertEqual("行っ", tokens[2]["token"])
        self.assertEqual(2, tokens[2]["position"])
        self.assertEqual(3, tokens[2]["start_offset"])
        self.assertEqual(5, tokens[2]["end_offset"])

        self.assertEqual("た", tokens[3]["token"])
        self.assertEqual(3, tokens[3]["position"])
        self.assertEqual(5, tokens[3]["start_offset"])
        self.assertEqual(6, tokens[3]["end_offset"])
        return


class TestICUFiltered(unittest.TestCase):
    # requires analysis-icu plugin installed
    def test_icu_filtered_stuff_is_not_trimmed(self):
        body = {
            "tokenizer": "sudachi_tokenizer",
            "char_filter": {
                "type": "icu_normalizer",
                "name": "nfkc_cf",
                "mode": "compose"
            },
            "text": "white",
        }
        resp = es_instance.analyze(body)
        self.assertEqual(200, resp.status, f"data: {resp.data}")

        tokens = json.loads(resp.data.decode())["tokens"]
        self.assertEqual(1, len(tokens))
        self.assertEqual("white", tokens[0]["token"])
        self.assertEqual(0, tokens[0]["position"])
        self.assertEqual(0, tokens[0]["start_offset"])
        self.assertEqual(5, tokens[0]["end_offset"])
        return


class TestSubplugin(unittest.TestCase):
    # requires :subplugin is installed with :testlib
    # requires test dictionary from `src/test/resources/dict/``
    def test_loads_config_and_plugin_from_subplugin(self):
        body = {
            "tokenizer": {
                "type": "sudachi_tokenizer",
                "settings_path": "sudachi_subplugin.json",
                # override to use test dictionary
                "additional_settings": "{\"systemDict\":\"system_test.dic\"}",
            },
            "text": "ゲゲゲの鬼太郎",
        }
        resp = es_instance.analyze(body)
        self.assertEqual(200, resp.status, f"data: {resp.data}")

        tokens = json.loads(resp.data)["tokens"]
        self.assertEqual(1, len(tokens), f"{tokens}")
        self.assertEqual("ゲゲゲの鬼太郎", tokens[0]["token"])
        self.assertEqual(0, tokens[0]["position"])
        self.assertEqual(0, tokens[0]["start_offset"])
        self.assertEqual(7, tokens[0]["end_offset"])
        return


if __name__ == "__main__":
    main()
