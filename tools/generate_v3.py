"""Generate YooKassaOperations and lossless typed JSON models (Python 3, stdlib only)."""
from pathlib import Path
import json
import re
import hashlib

ROOT = Path(__file__).resolve().parents[1]
SPEC_PATH = ROOT / 'openapi/yookassa.json'
SPEC = json.loads(SPEC_PATH.read_text(encoding='utf-8'))
SCHEMAS = SPEC['components']['schemas']
BASE = ROOT / 'src/main/java/ru/deelter/yookassa'
MODELS = BASE / 'model'
MODELS.mkdir(parents=True, exist_ok=True)
REGISTRY = {}

def pascal(value):
    return ''.join(p[:1].upper()+p[1:] for p in re.split(r'[^a-zA-Z0-9]', value) if p)

def deref(schema):
    if '$ref' in schema:
        result = SPEC
        for key in schema['$ref'][2:].split('/'):
            result = result[key]
        return result
    return schema

def flatten(schema, seen=()):
    if '$ref' in schema:
        ref = schema['$ref']
        if ref in seen:
            return {}
        return flatten(deref(schema), seen+(ref,))
    result = dict(schema)
    properties = {}
    required = []
    for part in schema.get('allOf', []):
        part = flatten(part, seen)
        properties.update(part.get('properties', {}))
        required.extend(part.get('required', []))
        for key,value in part.items():
            if key not in ('properties','required','allOf'):
                result.setdefault(key,value)
    properties.update(schema.get('properties', {}))
    required.extend(schema.get('required', []))
    if properties:
        result['properties'] = properties
        result['type'] = 'object'
    result['required'] = list(dict.fromkeys(required))
    return result

def register(name, schema):
    name = pascal(name)
    if name in REGISTRY and REGISTRY[name] != schema:
        raise ValueError('Conflicting model name: '+name)
    REGISTRY[name] = schema
    return name

def java_type(schema, suggested):
    if '$ref' in schema:
        name = schema['$ref'].split('/')[-1]
        target = flatten(schema)
        if target.get('type') == 'object' or 'properties' in target:
            return register(name, SCHEMAS[name])
        return java_type(target, suggested)
    # Description-only allOf wrappers retain the referenced model's public name.
    meaningful = [x for x in schema.get('allOf', []) if '$ref' in x or 'type' in x or 'properties' in x]
    if len(meaningful) == 1 and not schema.get('properties'):
        return java_type(meaningful[0], suggested)
    flat = flatten(schema)
    if 'oneOf' in flat or 'anyOf' in flat:
        for variant in flat.get('oneOf',flat.get('anyOf',[])):
            java_type(variant, suggested+'Variant')
        return 'JsonModel'
    kind = flat.get('type')
    if kind == 'array':
        return 'java.util.List<'+java_type(flat.get('items',{}),suggested+'Item')+'>'
    if kind == 'object' or 'properties' in flat:
        if flat.get('properties'):
            return register(suggested,schema)
        return 'com.google.gson.JsonObject'
    return {'string':'String','integer':'Long','number':'java.math.BigDecimal','boolean':'Boolean'}.get(kind,'com.google.gson.JsonElement')

OPERATIONS = {
('post','/payments'):'createPayment', ('get','/payments'):'getPayments',
('get','/payments/{payment_id}'):'getPayment', ('post','/payments/{payment_id}/capture'):'capturePayment',
('post','/payments/{payment_id}/cancel'):'cancelPayment',
('post','/payment_methods'):'createPaymentMethod', ('get','/payment_methods/{payment_method_id}'):'getPaymentMethod',
('post','/invoices'):'createInvoice', ('get','/invoices/{invoice_id}'):'getInvoice',
('post','/refunds'):'createRefund', ('get','/refunds'):'getRefunds', ('get','/refunds/{refund_id}'):'getRefund',
('post','/receipts'):'createReceipt', ('get','/receipts'):'getReceipts', ('get','/receipts/{receipt_id}'):'getReceipt',
('post','/deals'):'createDeal', ('get','/deals'):'getDeals', ('get','/deals/{deal_id}'):'getDeal',
('post','/payouts'):'createPayout', ('get','/payouts'):'getPayouts', ('get','/payouts/search'):'searchPayouts',
('get','/payouts/{payout_id}'):'getPayout', ('get','/sbp_banks'):'getSbpBanks',
('post','/personal_data'):'createPersonalData', ('get','/personal_data/{personal_data_id}'):'getPersonalData',
('post','/webhooks'):'createWebhook', ('get','/webhooks'):'getWebhooks', ('delete','/webhooks/{webhook_id}'):'deleteWebhook',
('get','/me'):'getSettings', ('post','/pos_links'):'createPosLink',
('post','/pos_links/{pos_link_id}/recipient'):'changePosLinkRecipient',
('post','/pos_links/{pos_link_id}/deactivate'):'deactivatePosLink',
('post','/pos_links/{pos_link_id}/activate'):'activatePosLink', ('get','/pos_links/{pos_link_id}'):'getPosLink'
}

for name,schema in SCHEMAS.items():
    if flatten(schema).get('type') == 'object':
        register(name,schema)

methods = []
manifest = []
for path,item in SPEC['paths'].items():
    for verb,operation in item.items():
        if verb not in ('get','post','delete','put','patch'):
            continue
        method = OPERATIONS[(verb,path)]
        params = [deref(x) for x in item.get('parameters',[])+operation.get('parameters',[])]
        path_params = re.findall(r'\{([^}]+)\}',path)
        query_params = [x for x in params if x['in']=='query']
        body_schema = deref(operation.get('requestBody',{})).get('content',{}).get('application/json',{}).get('schema')
        body_type = java_type(body_schema,body_schema.get('title',pascal(method)+'Request')) if body_schema else None
        response = next((deref(v) for k,v in operation['responses'].items() if k.startswith('2')), {})
        response_schema = response.get('content',{}).get('application/json',{}).get('schema')
        response_type = java_type(response_schema,pascal(method)+'Response') if response_schema else 'void'
        if method=='deleteWebhook': response_type='void'
        # Anonymous empty response objects still have a lossless model representation.
        if response_type=='com.google.gson.JsonObject': response_type='JsonModel'
        query_type = None
        if query_params:
            query_type = register(pascal(method)+'Query',{'type':'object','properties':{x['name']:x['schema'] for x in query_params}})
        args = [('String',pascal(x)[:1].lower()+pascal(x)[1:]) for x in path_params]
        if query_type: args.append((query_type,'query'))
        if body_type: args.append((body_type,'body'))
        if verb!='get': args.append(('String','idempotenceKey'))
        signature = ', '.join(t+' '+n for t,n in args)
        lines = ['\t/** '+operation['summary'].replace('*/','')+'. '+verb.upper()+' '+path+'. */',
                 '\tpublic '+response_type+' '+method+'('+signature+') throws IOException {']
        names = {raw: arg for raw,(_,arg) in zip(path_params,args)}
        segments = []
        for part in path.strip('/').split('/'):
            match = re.fullmatch(r'\{([^}]+)\}', part)
            segments.append('RequestEncoding.id('+names[match.group(1)]+')' if match else '"'+part+'"')
        if body_type:
            lines.append('\t\tObjects.requireNonNull(body, "body");')
        call = 'execute(HttpMethod.'+verb.upper()+', new String[] {'+', '.join(segments)+'}, '+('query' if query_type else 'null')+', '+('body' if body_type else 'null')+', '+('null' if verb=='get' else 'idempotenceKey')+', '+('null' if response_type=='void' else response_type+'.class')+')'
        lines.append('\t\t'+('' if response_type=='void' else 'return ')+call+';')
        lines.append('\t}\n')
        methods.append('\n'.join(lines))
        manifest.append({'method':method,'http_method':verb.upper(),'path':path,'arguments':args,'body':body_type,'query':query_type,'response':response_type,'query_parameters':[x['name'] for x in query_params]})

header='// Generated by tools/generate_v3.py. Do not edit by hand.\n'
(BASE/'YooKassaOperations.java').write_text(header+'''package ru.deelter.yookassa;

import java.io.IOException;
import java.util.Objects;
import ru.deelter.yookassa.model.*;

/**
 * Every operation of the pinned YooKassa OpenAPI snapshot. Use these methods through {@link YooKassa}.
 * Write operations require a caller-persisted idempotence key: reuse it only to retry the same operation.
 */
public abstract class YooKassaOperations {
	YooKassaOperations() {
	}

	abstract <T> T execute(HttpMethod method, String[] path, JsonModel query, JsonModel body,
			String idempotenceKey, Class<T> responseType) throws IOException;

'''+ '\n'.join(methods)+'}\n',encoding='utf-8')

def scalar_info(schema, seen=()):
    """Resolves references and single-part allOf wrappers of a scalar property."""
    if '$ref' in schema:
        if schema['$ref'] in seen: return {}
        return scalar_info(deref(schema), seen+(schema['$ref'],))
    meaningful = [x for x in schema.get('allOf', []) if '$ref' in x or 'type' in x or 'enum' in x]
    if len(meaningful) == 1 and not schema.get('properties'):
        merged = dict(scalar_info(meaningful[0], seen))
        for key in ('format', 'enum'):
            if key in schema: merged[key] = schema[key]
        return merged
    return schema

def constant(value):
    name = re.sub(r'[^A-Za-z0-9]+', '_', re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', str(value))).strip('_').upper()
    return ('_'+name if name[:1].isdigit() else name) or 'EMPTY'

def java_string(value):
    return '"'+value.replace('\\', '\\\\').replace('"', '\\"')+'"'

done=set()
while set(REGISTRY)-done:
    name=next(x for x in REGISTRY if x not in done)
    schema=flatten(REGISTRY[name])
    fields=[]
    constants=[]
    constant_names=set()
    properties=schema.get('properties',{})
    amount_like = 'value' in properties and 'currency' in properties and scalar_info(properties['value']).get('type') == 'string'
    for wire,prop in properties.items():
        suffix=pascal(wire)
        if suffix in ('Class','Json','Field'): suffix+='Value'
        kind=java_type(prop,name+suffix)
        required=wire in schema.get('required',[])
        token = 'new com.google.gson.reflect.TypeToken<'+kind+'>() {}.getType()' if '<' in kind else kind+'.class'
        fields.append('\t/** API field {@code '+wire+'}; '+('required' if required else 'optional')+'. Absent values return null. */\n'
                      '\tpublic '+kind+' get'+suffix+'() { return read("'+wire+'", '+token+'); }\n'
                      '\t/** Sets {@code '+wire+'}; null removes the field. */\n'
                      '\tpublic '+name+' set'+suffix+'('+kind+' value) { put("'+wire+'", value); return this; }\n')
        info=scalar_info(prop)
        if kind=='String' and info.get('format')=='date-time':
            fields.append('\t/** Parsed {@code '+wire+'}; the string accessor keeps the exact API value. */\n'
                          '\tpublic java.time.OffsetDateTime get'+suffix+'AsDateTime() { return readDateTime("'+wire+'"); }\n'
                          '\t/** Sets {@code '+wire+'} as an ISO-8601 date-time; null removes the field. */\n'
                          '\tpublic '+name+' set'+suffix+'AsDateTime(java.time.OffsetDateTime value) { putDateTime("'+wire+'", value); return this; }\n')
        if kind=='String' and info.get('format')=='date':
            fields.append('\t/** Parsed {@code '+wire+'}. */\n'
                          '\tpublic java.time.LocalDate get'+suffix+'AsDate() { return readDate("'+wire+'"); }\n'
                          '\t/** Sets {@code '+wire+'} as an ISO-8601 date; null removes the field. */\n'
                          '\tpublic '+name+' set'+suffix+'AsDate(java.time.LocalDate value) { putDate("'+wire+'", value); return this; }\n')
        if amount_like and wire=='value':
            fields.append('\t/** Exact decimal {@code value}. */\n'
                          '\tpublic java.math.BigDecimal getValueAsDecimal() { return readDecimal("value"); }\n'
                          '\t/** Sets {@code value} from an exact decimal without exponent notation. */\n'
                          '\tpublic '+name+' setValueAsDecimal(java.math.BigDecimal value) { putDecimal("value", value); return this; }\n'
                          '\t/** Creates an amount, for example {@code of(new BigDecimal("100.00"), "RUB")}. */\n'
                          '\tpublic static '+name+' of(java.math.BigDecimal value, String currency) {\n'
                          '\t\treturn new '+name+'().setValueAsDecimal(java.util.Objects.requireNonNull(value, "value"))\n'
                          '\t\t\t\t.setCurrency(java.util.Objects.requireNonNull(currency, "currency"));\n'
                          '\t}\n')
        enum_values = info.get('enum')
        if enum_values is None and info.get('type') == 'array':
            enum_values = scalar_info(info.get('items', {})).get('enum')
        for value in enum_values or []:
            if not isinstance(value, str): continue
            const = constant(wire)+'_'+constant(value)
            if const in constant_names: continue
            constant_names.add(const)
            constants.append('\t/** Documented value of {@code '+wire+'}. The API may return values added later. */\n'
                             '\tpublic static final String '+const+' = '+java_string(value)+';\n')
    text=header+'''package ru.deelter.yookassa.model;

/** Typed, lossless JSON view of the '''+name+''' schema. See the pinned OpenAPI specification. */
public class '''+name+''' extends JsonModel {
    public '''+name+'''() { super(); }
    public '''+name+'''(com.google.gson.JsonObject json) { super(json); }
'''+ ''.join(constants)+('\n' if constants else '')+'\n'.join(fields)+'}\n'
    (MODELS/(name+'.java')).write_text(text,encoding='utf-8')
    done.add(name)

(ROOT/'openapi/coverage.json').write_text(json.dumps({'spec_sha256':hashlib.sha256(SPEC_PATH.read_bytes()).hexdigest(),'operations':manifest,'models':sorted(done)},indent=2)+'\n',encoding='utf-8')
(ROOT/'docs').mkdir(exist_ok=True)
table = '# API v3 operation coverage\n\nGenerated from `openapi/yookassa.json`. Every operation in this snapshot is a method of `YooKassa`.\n\n'
table += '| HTTP | Path | Java method | Request model | Response model |\n| --- | --- | --- | --- | --- |\n'
for op in manifest:
    table += '| '+ ' | '.join([op['http_method'], '`'+op['path']+'`', '`'+op['method']+'`', op['body'] or op['query'] or 'None', op['response']])+' |\n'
table += '\nThe facade uses '+str(len(done))+' object models. Primitive aliases use Java strings, booleans, long integers and BigDecimal. Polymorphic unions use JsonModel and typed variant views.\n'
(ROOT/'docs/API_COVERAGE.md').write_text(table,encoding='utf-8')
print('Generated',len(manifest),'operations and',len(done),'object models.')
